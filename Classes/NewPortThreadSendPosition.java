package classes;


import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import sun.security.action.GetLongAction;


import backgroundTask.BackgroundServer;
import classes.MyServerSocketDynamicPorts;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.sun.xml.internal.bind.v2.TODO;

import java.util.concurrent.atomic.AtomicBoolean;


import com.kuka.roboticsAPI.controllerModel.Controller;


/**
 * Essa classe cria uma thread que envia constantemente para o client o posicionamento da flange
 * <p>
 * It sends the current cartesian position to Invesalius everytime it receives a message from Invesalius, so the frequency of the loop (while)
 * respects the frequency of Invesalius Navigator.
 * <p>
 * A estruturação dessa classe é um pouco peculiar: A classe implementa Runnable, mas cria uma Thread worksend para fazer a tarefa.
 * Assim utiliza as partes positivas tanto do Runnable quanto do Thread.
 * @author Victor
 * @see Thread
 * @see Runnable
 */
public class NewPortThreadSendPosition implements Runnable
{	
	//@Inject
	//Controller kUKA_Sunrise_Cabinet_1;
	
	//@Inject
	//@Named("TMS")
	private Tool ferramenta = null;
	
	@Inject
	private LBR lbr;
	private MyServerSocketDynamicPorts sendingServerSocket;
	
	private Thread workersend; //criand worksend que será instanciado no método start()
	private String message; //recebe o posicionamento x,y,z,a,b,c da flange
	private String error = ""; //string para receber erros, isso foi necessário pois não é possivel printar no log por esse script
	private String signal; //string receives a signal from Invesalius, so it can send the current position
	
	private AtomicBoolean running = new AtomicBoolean(false); //flag para parar a thread
	
	/**
	 * Essa thread utiliza a tool CanetaBIC e o LBR lbr
	 * @param lbr
	 * @param canetaBIC
	 */
	@Inject
	public NewPortThreadSendPosition(LBR lbr, Tool ferramenta)
	{
		this.lbr = lbr;
		this.ferramenta = ferramenta;
	}
	
	/**
	 * Instancia uma Thread e a inicia (realiza o que está no run())
	 * @see Thread
	 */
	public void start(){
		workersend = new Thread(this);
		workersend.start();
	}
	
	/**
	 * Torna a flag running false para sair do while, mas também interrompe o run()
	 * @see void java.lang.Thread.interrupt()
	 */
	public void interrupt() //não recomendado o seu uso, utilizar o stop()
	{
		if (sendingServerSocket.isConnected())
		{
			try {
				sendingServerSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		running.set(false);
		if(this.isRunning())
		{
			workersend.interrupt();
		}
	}
	
	/**
	 * Verifica se a Thread está rodando
	 * @return true or false
	 */
	public boolean isRunning()
	{
		if(workersend == null) 	//se worksend não foi instanciado, worksend.isAlive() vai dar erro
		{						//portanto precisou do if
			return false;
		}
		else
		{
			return workersend.isAlive();
		}
	}
	
	/**
	 * Torna a flag running == false e o join() força a main Thread a esperar essa aqui terminar
	 * @throws InterruptedException 
	 */
	public void stop() throws InterruptedException
	{
		running.set(false);
		workersend.join();
	}
	
	/**
	 * Retorna error para poder ser printado no log
	 * @return error string contendo o erro
	 */
	public String getError()
	{
		return error;
	}
	
	/**
	 * retorna a mensagem para verificar se está ok
	 * @return message String contendo as coordenadas da flange
	 */
	public String getMessage()
	{
		return message;
	}
	
	/**
	 * O run() que a Thread realiza
	 */
	@Override
	public void run()
	{
		//tentativa de criação do servidor ---------------------------------------------------------------------
		try {
			sendingServerSocket = new MyServerSocketDynamicPorts("0.0.0.0", 30000); //a porta da main é 30001
		} catch (Exception e) {
			// TODO Bloco catch gerado automaticamente
			e.printStackTrace();
			//se a criação falha
			try {
				this.stop();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		//Aguardando conexão pelo cliente
		try {
			sendingServerSocket.listen();
		} catch (Exception e1) {
			// TODO Bloco catch gerado automaticamente
			e1.printStackTrace();
			running.set(false);
			}
		// servidor criado e conectado ao client ------------------------------------------------------------------
		
		running.set(true); 
		//comentei o attach pq ja foi 
		//canetaBIC.attachTo(lbr.getFlange()); //attachTO fixes the tool to some frame, in this case it is attached to Flange
		while(running.get())
		{
			try {
				signal = sendingServerSocket.getMessage();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				running.set(false);
			} //the code only advance if receives a message from Invesalius
			
			//Receive the CanetaBic's position and send it to PythonClient
			message =  	Double.toString(lbr.getCurrentCartesianPosition(ferramenta.getFrame("Ponta")).getX()) 		+ " " +
						Double.toString(lbr.getCurrentCartesianPosition(ferramenta.getFrame("Ponta")).getY()) 		+ " " +
						Double.toString(lbr.getCurrentCartesianPosition(ferramenta.getFrame("Ponta")).getZ()) 		+ " " +
						Double.toString(lbr.getCurrentCartesianPosition(ferramenta.getFrame("Ponta")).getAlphaRad()) 	+ " " +
						Double.toString(lbr.getCurrentCartesianPosition(ferramenta.getFrame("Ponta")).getBetaRad()) 	+ " " +
						Double.toString(lbr.getCurrentCartesianPosition(ferramenta.getFrame("Ponta")).getGammaRad()) + " ";
			try {
				sendingServerSocket.sendMessage(message);
			} catch (IOException e1) {
				running.set(false);
				e1.printStackTrace();
			}
			
		}
		try {
			sendingServerSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
