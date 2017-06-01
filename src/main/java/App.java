import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ConcurrentHashSet;
import src.User;

import java.util.*;
import java.util.stream.Collectors;


public class App 
{
    private static int MSG_BLOCK_SIZE=50;
    private static final String  WS_URL_TAIL = "/ws";//http - регистрация пользоваеля и открытие соединения
    private static final String ADD_MES_TAIL = "/api/msg";//получение сообщений
    private static Object key=new Object();
    private static boolean isGet = true;
    private static Set<ServerWebSocket> allUsrs = new ConcurrentHashSet<>();
     private static ArrayList<String> allMessages = new ArrayList<>();


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
            //stringBuilder.append("[");
            if (allMessages.size()!=0){
                System.out.println("Текущие сообщения");
               printList(allMessages);
//               Collections.reverse(allMessages);
//                System.out.println("После реверса");
//                printList(allMessages);

//                if (isGet){
//                    Collections.reverse(allMessages);
//                    isGet = false;
//                    System.out.println("После реверса");
//                    printList(allMessages);
//                }

                if (loadCount>allMessages.size()){// запросили больше чем есть на сервере в данн момент
                    for (int i = 0;i<allMessages.size();i++){
                        stringBuilder.append(allMessages.get(i));
                        if (i!=allMessages.size()-1){
                            stringBuilder.append(";");
                        }
                    }
                }
                else {
                    List<String> toClient = allMessages.stream().skip(allMessages.size()-loadCount).collect(Collectors.toList());
                    Collections.reverse(toClient);
                    System.out.println("Ушло клиенту");
                    for (int i=0;i<loadCount;i++){
                        String elem = toClient.get(i);//allMessages.get(key);
                        printElem(elem);
                        stringBuilder.append(elem);
                        if (i!=loadCount-1){
                            stringBuilder.append(";");
                        }
                    }
                }
            }
            //stringBuilder.append("]");
            return stringBuilder.toString();
        }


    }
    public static void putMessage(String message){
        synchronized(key){
            isGet=true;
            long date = System.currentTimeMillis();
            allMessages.add(message);
            System.out.println("Добавлено сообщение с датой"+new Date(date).toString());
        }
    }
    public static void printList(ArrayList<String> allMessages){
        for (String user:allMessages){
            System.out.println(user);
        }
    }

    public static void printElem(String user){
        System.out.println(user);
    }

}
