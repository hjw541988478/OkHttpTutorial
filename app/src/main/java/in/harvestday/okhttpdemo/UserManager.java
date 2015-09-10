package in.harvestday.okhttpdemo;

/**
 * Created by Administrator on 2015/9/10.
 */
public class UserManager {

    private static UserManager manager;

    private UserManager() {
    }

    public static UserManager getInstance() {
        if (manager == null)
            synchronized (UserManager.class) {
                if (manager == null)
                    manager = new UserManager();
            }
        return manager;
    }


    public void add() {

    }

    public void delete() {

    }

    public void update() {

    }

    public void findAll() {

    }
}

