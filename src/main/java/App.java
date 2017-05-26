import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Set;
import java.util.TreeMap;


public class App 
{
    private static final int MSG_BLOCK_SIZE=50;
    private static final String  WS_URL_TAIL = "/ws";//http
    private static final String ADD_MES_TAIL = "/api/msg";
    private static Object key=new Object();
    private static Set<ServerWebSocket> allUsrs = new ConcurrentHashSet<>();
     private static TreeMap<Long,String> allMessages = new TreeMap<>((o1, o2)->{
        return Long.compare(o2,o1);
    });


    public static void main( String[] args ) {
        Vertx core = Vertx.vertx();
        HttpServer httpServer = core.createHttpServer();
        System.out.println("Server created");


        httpServer.requestHandler(request -> {//обр зап
            String tail = request.path();//
            switch (tail) {
                case WS_URL_TAIL:
                    ServerWebSocket socket = request.upgrade();
                    allUsrs.add(socket);// извери - это сокет
                    socket.textMessageHandler(msg -> {
                        putMessage(msg);
                        allUsrs.forEach(client -> {
                            client.writeTextMessage(msg);
                        });
                    });
                    socket.endHandler(aVoid -> {
                        allUsrs.remove(socket);
                    });
                    socket.closeHandler(aVoid -> {
                        allUsrs.remove(socket);
                    });
                    break;
               /*-----------------------------------------------------*/
                case ADD_MES_TAIL:
                    String resp = null;
                    try {
                        String messageBody = request.getParam("from");
                        Long value = Long.parseLong(messageBody);
                        resp = getMessages(value);
                    } catch (Exception e) {
                        resp = "[]";
                    }
                    request.response()
                            .putHeader("Access-Control-Allow-Origin", "*")
                            .end(resp);
                    break;
                default:
                    request.response().setStatusCode(404).end();
                    break;

            }

        }).listen(8080);
        System.out.println("Starting");


    }

    public static String getMessages(long end){
        synchronized (key){// зверей мб много
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            int count=0;
            Set<Long> keys = allMessages.subMap(end,false,0L,false).keySet();

            for(Long key: keys){
                String msg = allMessages.get(key);
                stringBuilder.append(msg);
                count++;
                if(count==MSG_BLOCK_SIZE || count == keys.size()){
                    break;
                }
                stringBuilder.append(",");
            }
            stringBuilder.append("]");
            return stringBuilder.toString();
        }


    }
    public static void putMessage(String message){
        synchronized(key){
            allMessages.put(System.currentTimeMillis(),message);
        }
    }

}
