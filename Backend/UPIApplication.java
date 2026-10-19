public class UPIApplication {

    public static void main(String[] args) throws Exception {

        PaymentService paymentService = new PaymentService();

        Thread tcpThread = new Thread(
                () -> TCPServer.start(paymentService));

        tcpThread.start();

        UPIHttpServer.start(paymentService);
    }
}