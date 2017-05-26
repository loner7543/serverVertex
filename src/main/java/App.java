import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.impl.launcher.commands.ExecUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Hello world!
 *
 */
public class App 
{
    static private Object key=new Object();
    static private Set<ServerWebSocket> clients = new ConcurrentHashSet<>();
    static private TreeMap<Long,String> messages  = new TreeMap<>((o1,o2)->{
        return Long.compare(o2,o1);
    });
    private static final int MSG_BLOCK_SIZE=50;
    public static void main( String[] args )
    {
       final Vertx vertx = Vertx.vertx();
       final HttpServer server = vertx.createHttpServer();


       server.requestHandler(request -> {
           String path = request.path();
           switch (path){
               case "/ws":
                   ServerWebSocket webSocket = request.upgrade();
                   clients.add(webSocket);
                   webSocket.textMessageHandler(msg->{
                       putMessage(msg);
                       clients.forEach(client->{
                           client.writeTextMessage(msg);
                       });
                       System.out.println("[NEW MESSAGE]:"+msg);
                   });
                   webSocket.endHandler(aVoid->{
                       clients.remove(webSocket);
                   });
                   webSocket.closeHandler(aVoid -> {
                       clients.remove(webSocket);
                   });
                   break;
               /*-----------------------------------------------------*/
               case "/api/msg":
                   String responseString=null;
                   try {
                       String svalue = request.getParam("from");
                       Long value = Long.parseLong(svalue);
                       responseString = getMessages(value);
                   }catch(Exception e){
                       responseString="[]";
                   }
                   request.response()
                           .putHeader("Access-Control-Allow-Origin","*")
                           .end(responseString);
                   break;
               default:
                   request.response().setStatusCode(404).end();
                   break;

           }

       }).listen(8080);


    }
    public static void putMessage(String message){
        synchronized(key){
           messages.put(System.currentTimeMillis(),message);
        }
    }

    public static String getMessages(long end){
        synchronized (key){
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            int count=0;
            Set<Long> keys = messages.subMap(end,false,0L,false).keySet();

            for(Long key: keys){
                String msg = messages.get(key);
                sb.append(msg);
                count++;
                if(count==MSG_BLOCK_SIZE || count == keys.size()){
                    break;
                }
                sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }


    }
}
