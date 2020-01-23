package classes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.inject.Inject;

//import javax.inject.Inject;

//import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;

/**
 * Essa classe foi criada para realizar comunicação entre o Iiwa e uma máquina externa;
 * @see ServerSocket
 * @see Socket
*/
public class MyServerSocketDynamicPorts
{
    private ServerSocket server;
    private Socket client;
    private String clientAddress;
    private BufferedReader in;
    private String messageToSend;
    
    /**
     * Construtor, recebe ipAdress (colocar "0.0.0.0" para acessar por qualquer máquina).
     * Caso ipAdress == null, ele irá utilizar "0.0.0.0"
     * O port está definido como 30000
     * @param ipAddress o parâmetro que recebe o endereço de ip
     * @throws Exception
     */
    @Inject
    public MyServerSocketDynamicPorts(String ipAddress, Integer port) throws Exception
    {
    	this.server = new ServerSocket();
    	this.server.setReuseAddress(true);
    	
        if (ipAddress != null && !ipAddress.isEmpty())
        {
        	this.server.bind(new InetSocketAddress(ipAddress, port));
        }
        else
        {
        	this.server.bind(new InetSocketAddress("0.0.0.0", port));
        }
    }
    
    /**
     * configura o socket client, que será utilizado para receber e enviar dados
     * @throws Exception
     */
    public void listen() throws Exception
    {
    	client = null;
        client = this.server.accept();
        clientAddress = client.getInetAddress().getHostAddress();
        System.out.println("\r\nNew connection from " + clientAddress);
    }
    
    public InetAddress getSocketAddress()
    {
        return this.server.getInetAddress();
    }
    
    public int getPort()
    {
        return this.server.getLocalPort();
    }
    
    /**
     * Retorna o estado da conexão do servidor, como client.isConnected() apenas informa se houve conexão, tentou-se utilizar client.isClosed() para isso
     * <p>
     * TODO: verificar se esse método está funcionando, da ultima vez que checamos, ele dava true msm com o servidor fechado
     * @see boolean java.net.Socket.isConnected()
     * @see boolean java.net.Socket.isClosed()
     * @return (isconnected && !isclosed) Teoricamente retornaria true apenas se o servidor estiver conectado
     */
    public Boolean isConnected()
    {
    	boolean isconnected = client.isConnected();
    	boolean isclosed = client.isClosed();
    	return (isconnected && !isclosed);
    }

	/**
	 * fecha primeiro o Socket e depois o ServerSocket
	 * @see void java.net.Socket.close()
	 * @see void java.net.ServerSocket.close()
	 * @throws IOException
	 */
    public void close() throws IOException
    {
    	if(server.isBound())
    	{
    		if(client != null)
    		{
    			client.close();
    		}
    		server.close();
    	}
    }
    
    /**
     * Retorna a mensagem recebida em forma de string
     * @return
     * @throws IOException
     */
	public String getMessage() throws IOException
	{
		// TODO Apêndice de método gerado automaticamente
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		String str = in.readLine();
		return str;
	}
	
	/**
	 * Envia a mensagem
	 * @param messageToSend string contendo a mensagem a ser enviada
	 * @throws IOException
	 */
	public void sendMessage(String messageToSend) throws IOException
	{
		this.messageToSend = messageToSend;
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
		out.write(messageToSend);
		out.flush();
	}
	
}