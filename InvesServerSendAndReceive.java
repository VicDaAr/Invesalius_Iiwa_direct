package application;

import java.io.IOException;

import classes.MyServerSocketDynamicPorts;
import classes.NewPortThreadSendPosition;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

/**
 * This application was made to work with InvesaliusNavigator, using the robot Iiwa as a tracker for STATIC navigation
 * <p>
 * It sends the current cartesian position to Invesalius everytime it receives a message from Invesalius, so the frequency of the loop (while)
 * respects the frequency of Invesalius Navigator.
 * <p>
 * In the main code it receives coordinate data from Invesalius, realizing a PTP moviment to the desired frame(pint in the space), 
 * the NewPortThreadSendPosition is a thread for sending current tool position.
 * <p>
 * Sending PortID: 30000
 * Receiving PortID: 30001 
 * <b>It is imperative to call <code>super.dispose()</code> when overriding the 
 * {@link RoboticsAPITask#dispose()} method.</b> 
 * 
 * @see UseRoboticsAPIContext
 * @see #initialize()
 * @see #run()
 * @see #dispose()
 */
public class InvesServerSendAndReceive extends RoboticsAPIApplication {

	@Inject
	private LBR lbr; //criando o objeto robô
	
	@Inject
	@Named("CanetaBIC")
	private Tool canetaBIC; //criando o objeto canetaBIC

	private MyServerSocketDynamicPorts serverSocket; //serverSocket for methods related to server
	
	private NewPortThreadSendPosition sendPosition; //thread responsable for sending current Iiwa position
	
	private Boolean endProgram = false; //Boolean para encerrar o programa caso a criação do servidor falhe
	
	boolean running; //Boolean que mantem o while rodando
	
	String command, message; //command recebe yes or no para saber se vai rodar o while novamente
	
	String str; //str recebe as coordenadas x,y,z,a,b,c
	
	double x,y,z,a,b,c; //x,y,z referentes ao root e a,b,c à rotação da base da flange
	
	Frame xablau = new Frame(); //Frame que recebe os valores do cliente e ordena a movimentação
	
	//portID and ipAddress to initialize server
	private Integer portIDReceive = 30001;
	private String ipAddress = "0.0.0.0";
	
	
	@Override
	public void initialize()
	{	
		// initialize your task here
		
		canetaBIC.attachTo(lbr.getFlange()); //attachTO fixes the tool to some frame, in this case it is attached to Flange
		
		//setting frame xablau 
		xablau.setParent(lbr.getRootFrame());
		
		/* 
		xablau.setX(lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta")).getX());
		xablau.setY(lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta")).getY());
		xablau.setZ(lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta")).getZ());
		xablau.setAlphaRad(lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta")).getAlphaRad());
		xablau.setBetaRad(lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta")).getBetaRad());
		xablau.setGammaRad(lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta")).getGammaRad());
		*/
		
		//creating Thread and starting it
		sendPosition = new NewPortThreadSendPosition(lbr, canetaBIC);
		sendPosition.start();
		
		//tentativa de criação do servidor -------------------------------------------
		try {
			serverSocket = new MyServerSocketDynamicPorts(ipAddress, portIDReceive);
		} catch (Exception e) {
			// TODO Bloco catch gerado automaticamente
			e.printStackTrace();
			getLogger().info(e.getMessage());
			try {
				sendPosition.stop();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			endProgram = true; //se a criação falha
		}
		//encerrada a criação do servidor -------------------------------------------------
	}
	
	@Override
	public void run()
	{
		
		//if que encerra o programa caso ocorra falha no estabelecimento do servidor
		if(endProgram)
		{
			getLogger().info("Ending program!");
			return;
		}
		
		running = true; //Boolean para posteriormente sair do while
		
		//Aguardando conexão pelo cliente-----------------------------------------------------
		getLogger().info("Trying to listen...");
		try {
			serverSocket.listen();
			getLogger().info("Waiting connection...");
		} catch (Exception e1) {
			// TODO Bloco catch gerado automaticamente
			e1.printStackTrace();
			running = false;
			try {
				sendPosition.stop();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.dispose();
		}
		//Client conection ok! -----------------------------------------------------------------
		
		
		//Inicio do programa
		//IMPORTANTE: isConnected apenas verifica se houve conexão e não se está conectado!!!!
		if(serverSocket.isConnected())
		{
			getLogger().info("Connected!");
			getLogger().info("Connection from: " + serverSocket.getSocketAddress() + Integer.toString(serverSocket.getPort()));
			
			//While para repetir quantas vezes o usuário desejar
			while(running)
			{
				try{
						
					try
					{
						str = serverSocket.getMessage(); //recebendo as coordenadas
						getLogger().info("Mensagem recebida: " + str);
					}
					catch (Exception e)
					{
						// TODO Bloco catch gerado automaticamente
						e.printStackTrace();
						getLogger().info("entro no catch");
						running = false;
					}
					
					String[] splittedStr = str.split(" "); //as coordenadas foram enviadas com um " " entre elas, o split está separando-as
					
					//nomeando as coordenadas
					x = Double.parseDouble(splittedStr[0]);
					y = Double.parseDouble(splittedStr[1]);
					z = Double.parseDouble(splittedStr[2]);
					a = Double.parseDouble(splittedStr[3]);
					b = Double.parseDouble(splittedStr[4]);
					c = Double.parseDouble(splittedStr[5]);
					
					getLogger().info("a = " + String.valueOf(a) + "b = " + String.valueOf(b) + "c = " + String.valueOf(c));
					
					//definindo as coordenada do xablau
					xablau.setParent(lbr.getRootFrame()); 
					xablau.setX(x);
					xablau.setY(y);
					xablau.setZ(z);
					if (a != 0 && b != 0 && c != 0) //verificando o valor das rotações
					{
						getLogger().info("Entrou no if a,b,c != 0");
						xablau.setAlphaRad(a);
						xablau.setBetaRad(b);
						xablau.setGammaRad(c);
					}
					else //caso o valor das rotações sejam 0, pegar o valor atual
					{
						getLogger().info("Entrou no else");
						xablau.setAlphaRad(lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta")).getAlphaRad());
						xablau.setBetaRad(lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta")).getBetaRad());
						xablau.setGammaRad(lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta")).getGammaRad());
					}
					
					
					//confirmação do movimento, "Movimentar" => cmd = 0 ; "Cancelar" => cmd = 1
					int cmd = getApplicationUI().displayModalDialog(ApplicationDialogType.INFORMATION, "O Iiwa irá se mover para o ponto: " + xablau.toString() , "Movimentar", "Cancelar");
					if(cmd == 0)
					{
						try //try and catch para que caso a movimentação falhe, ele não pare o problema
						{
							lbr.getCurrentCartesianPosition(canetaBIC.getFrame("Ponta"));
							canetaBIC.getFrame("Ponta").move(ptp(xablau));
							getLogger().info("Movimentou para " + xablau.toString());
						}
						catch (Exception movError)
						{
							getLogger().info("Movimentacao falhou");
							getLogger().info(movError.toString());
						}
					}
					else
					{
						getLogger().info("Não movimentou");
					}
			}
			catch(Exception Error){
				running = false;
			}
				
			} //saindo do while
			
			//parando a thread e desligando o servidor
			try {
				serverSocket.close();
				try {
					sendPosition.stop();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e) {
				// TODO Bloco catch gerado automaticamente
				e.printStackTrace();
			}
		} //saindo do if
	}
	
	//Se apertar o botão para fechar a aplicação ele entra no dispose
	@Override
	public void dispose()
	{
		//parando a thread
		try {
			serverSocket.close();
			sendPosition.stop();
		} catch (IOException e) {
			// TODO Bloco catch gerado automaticamente
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getLogger().info("(ServerSocket is free)");
		super.dispose();
	}
}