package android.app;

import java.util.List;

/**
 * Created by thom on 2017/2/17.
 */
public interface IActivityManager {

    List getRecentTasks(int maxNum, int flags, int userId);

}
