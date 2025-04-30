package org.kickmyb.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kickmyb.server.account.BadCredentialsException;
import org.kickmyb.server.account.MUser;
import org.kickmyb.server.account.MUserRepository;
import org.kickmyb.server.account.ServiceAccount;
import org.kickmyb.server.task.ServiceTask;
import org.kickmyb.transfer.AddTaskRequest;
import org.kickmyb.transfer.SignupRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO pour celui ci on aimerait pouvoir mocker l'utilisateur pour ne pas avoir à le créer

// https://reflectoring.io/spring-boot-mock/#:~:text=This%20is%20easily%20done%20by,our%20controller%20can%20use%20it.

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = KickMyBServerApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@ActiveProfiles("test")
class ServiceTaskTests {

    @Autowired
    private MUserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServiceTask serviceTask;

    @Autowired
    private ServiceAccount serviceAccount;



    @Test
    void testAddTask() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        serviceTask.addOne(atr, u);

        assertEquals(1, serviceTask.home(u.id).size());
    }

    @Test
    void testAddTaskEmpty()  {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Empty");
        } catch (Exception e) {
            assertEquals(ServiceTask.Empty.class, e.getClass());
        }
    }

    @Test
    void testAddTaskTooShort() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "o";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.TooShort");
        } catch (Exception e) {
            assertEquals(ServiceTask.TooShort.class, e.getClass());
        }
    }

    @Test
    void testAddTaskExisting() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Bonne tâche";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Existing");
        } catch (Exception e) {
            assertEquals(ServiceTask.Existing.class, e.getClass());
        }
    }

    // Vérifier que la suppression fonctionne avec un ID correct
    @Test
    void testDeleteTaskIdOk() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing, ServiceTask.TaskNotOwnedByUser,
            ServiceTask.TaskNotFound, ServiceAccount.UsernameTooShort, ServiceAccount.PasswordTooShort, ServiceAccount.UsernameAlreadyTaken,
            BadCredentialsException {
        // Créer un utilisateur
        SignupRequest sr = new SignupRequest();
        sr.username = "m. test";
        sr.password = "passw0rd!";
        serviceAccount.signup(sr); // Utilisation d'une injection de dépendance
        MUser u = userRepository.findByUsername(sr.username).get();

        // Créer une tâche
        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        // Ajouter la tâche
        serviceTask.addOne(atr, u);
        // Vérifier qu'il y a bien une tâche
        assertEquals(1, serviceTask.home(u.id).size());

        // Recharger l'utilisateur après lui avoir ajouté une tâche : mettre à le u local
        u = userRepository.findById(u.id).get();

        // Récupérer l'ID de la tâche
        long taskId = serviceTask.home(u.id).get(0).id;
        // Supprimer la tâche
        serviceTask.delete(taskId, u);
        // Vérifier qu'il n'y a plus de tâche
        assertEquals(0, serviceTask.home(u.id).size());
    }

    // Vérifier que la suppression ne fonctionne pas avec un ID incorrect
    @Test
    void testDeleteTaskWrongId() throws ServiceAccount.UsernameTooShort, ServiceAccount.PasswordTooShort, ServiceAccount.UsernameAlreadyTaken, BadCredentialsException, ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing, ServiceTask.TaskNotOwnedByUser, ServiceTask.TaskNotFound {
        // Créer un utilisateur
        SignupRequest sr = new SignupRequest();
        sr.username = "m. test";
        sr.password = "passw0rd!";
        serviceAccount.signup(sr); // Utilisation d'une injection de dépendance
        MUser u = userRepository.findByUsername(sr.username).get();

        // Créer une tâche
        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        // Ajouter la tâche
        serviceTask.addOne(atr, u);
        // Vérifier qu'il y a bien une tâche
        assertEquals(1, serviceTask.home(u.id).size());

        // Recharger l'utilisateur après lui avoir ajouté une tâche : mettre à le u local
        u = userRepository.findById(u.id).get();

        // Récupérer l'ID de la tâche
        long taskId = serviceTask.home(u.id).get(0).id;
        // Supprimer la tâche
        try {
            serviceTask.delete(taskId+1, u);
        } catch (Exception e) {
        }
        assertEquals(1, serviceTask.home(u.id).size());
    }

    // Vérifier que la suppression ne fonctionne pas avec un utilisateur incorrect
    @Test
    void testDeleteTaskWrongUser() throws ServiceAccount.UsernameTooShort, ServiceAccount.PasswordTooShort, ServiceAccount.UsernameAlreadyTaken, BadCredentialsException, ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing, ServiceTask.TaskNotOwnedByUser, ServiceTask.TaskNotFound {
        // Créer Alice
        SignupRequest sr = new SignupRequest();
        sr.username = "alice";
        sr.password = "passw0rd!";
        serviceAccount.signup(sr); // Utilisation d'une injection de dépendance
        MUser alice = userRepository.findByUsername(sr.username).get();

        // Créer une tâche
        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de alice";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        // Ajouter la tâche
        serviceTask.addOne(atr, alice);
        // Vérifier qu'il y a bien une tâche
        assertEquals(1, serviceTask.home(alice.id).size());

        // Créer Bob
        SignupRequest sr2 = new SignupRequest();
        sr2.username = "bob";
        sr2.password = "passw0rd!";
        serviceAccount.signup(sr2); // Utilisation d'une injection de dépendance
        MUser bob = userRepository.findByUsername(sr2.username).get();

        // Essayer de supprimer la tâche de Alice avec Bob
        try {
            serviceTask.delete(serviceTask.home(alice.id).get(0).id, bob);
        } catch (Exception e) {
        }

        // Vérifier que la tâche de Alice est toujours là
        assertEquals(1, serviceTask.home(alice.id).size());
    }


}
