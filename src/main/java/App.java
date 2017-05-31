import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ConcurrentHashSet;
import src.User;

import java.util.*;


public class App 
{
    private static int MSG_BLOCK_SIZE=50;
    private static final String  WS_URL_TAIL = "/ws";//http - регистрация пользоваеля и открытие соединения
    private static final String ADD_MES_TAIL = "/api/msg";//получение сообщений
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
                        long loadCount=Long.parseLong(request.getParam("count"));
                        Long value = Long.parseLong(messageBody);
                        resp = getMessages(value,loadCount);
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

    public static String getMessages(long end,long loadCount){// в параметрах - дата окончания
        synchronized (key){// зверей мб много
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            ArrayList<User> messages = new ArrayList<>();
            if (allMessages.size()!=0){
                for (long key :allMessages.keySet()){
                    String mes = allMessages.get(key);
                    messages.add(new User(key,mes));

                }
                Collections.reverse(messages);
                if (loadCount>messages.size()){// запросили больше чем есть на сервере в данн момент
                    for (int i = 0;i<messages.size();i++){
                        stringBuilder.append(messages.get(i).getMes());
                        if (i!=messages.size()-1){
                            stringBuilder.append(",");
                        }
                    }
                }
                else {
                    for (int i=0;i<loadCount;i++){
                        User elem = messages.get(i);//allMessages.get(key);
                        stringBuilder.append(elem.getMes());
                        if (i!=loadCount-1){
                            stringBuilder.append(",");
                        }
                    }
                }
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
