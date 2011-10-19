package play.template2



/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/19/11
 * Time: 8:09 PM
 * To change this template use File | Settings | File Templates.
 */
class a implements Runnable{

    void run() {
        def list = ["a", "b"];
        list.each() {
            println( it);
        }

    }
}

