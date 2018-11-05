package mytomcat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyBIOTomcat {
    private int port = 8080;
    private static Map<String, String> urlServletMapping = new HashMap<>();

    public MyBIOTomcat(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        new MyBIOTomcat(8088).start();
    }

    public void start() {
        initServletMapping();

        ExecutorService executor = Executors.newFixedThreadPool(100);//线程池

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                executor.submit(new ConnectIOHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void dispatch(MyRequest myRequest, MyResponse myResponse) {
        String clazz = urlServletMapping.get(myRequest.getUrl());

        try {
            Class<MyServlet> myServletClass = (Class<MyServlet>) Class.forName(clazz);
            MyServlet myServlet = myServletClass.newInstance();
            myServlet.service(myRequest, myResponse);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    private static void initServletMapping() {
        for (ServletMapping servletMapping : ServletMappingConfig.servletMappingList) {
            urlServletMapping.put(servletMapping.getUrl(), servletMapping.getClazz());
        }
    }
}

class ConnectIOHandler extends Thread{
    private Socket socket;

    public ConnectIOHandler(Socket socket){
        this.socket = socket;
    }
    public void run(){
        while(!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
            InputStream inputStream = null;
            try {
                inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();
                MyRequest myRequest = new MyRequest(inputStream);
                MyResponse myResponse = new MyResponse(outputStream);

                MyBIOTomcat.dispatch(myRequest, myResponse);
                System.out.println(Thread.currentThread().getName());

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}