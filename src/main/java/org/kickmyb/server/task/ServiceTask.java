package org.kickmyb.server.task;

import org.kickmyb.server.account.MUser;
import org.kickmyb.transfer.*;

import java.util.List;

public interface ServiceTask {


    class Existing extends Exception {}
    class TooShort extends Exception {}
    class Empty extends Exception {}
    class TaskNotFound extends Exception {}
    class TaskNotOwnedByUser extends Exception {}

    // entity handling
    TaskDetailResponse detail(Long id, MUser user);
    void addOne(AddTaskRequest req, MUser user) throws Existing, Empty, TooShort;
    void updateProgress(long taskID, int value, MUser user);
    List<HomeItemResponse> home(Long userID);
    TaskDetailPhotoResponse detailPhoto(Long id, MUser user);
    List<HomeItemPhotoResponse> homePhoto(Long userID);
    void delete(long id, MUser user) throws TaskNotFound, TaskNotOwnedByUser;

    // Potential web demo for JS injection
    String index();

    MUser userFromUsername(String username);
}