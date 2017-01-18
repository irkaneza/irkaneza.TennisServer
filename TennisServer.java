import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TennisServer implements Runnable
{	
	private List<Player> playerList;
	private String[] IPList;
	private int port = 12345;
	private ServerSocket serverSocket;
	private Socket playerSocket;
	
	
	public TennisServer(int port) throws IOException
	{
		this.port = port;
		serverSocket = new ServerSocket(this.port);
		playerList = new ArrayList<Player>();
	}
	
	public static void main(String[] args) throws IOException 
	{
		TennisServer tennisServer = new TennisServer(12345);
		Thread serverThread = new Thread(tennisServer);
		serverThread.start();		
	}
	
	private void writeToLog(String logString, Socket host) throws IOException
	{
		
		Date date = new Date();
        SimpleDateFormat formatDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String log = formatDate.format(date) + "\t" 
        				+ logString + "\t" 
        				+ host.getInetAddress().getHostAddress() 
        				+ ":" + host.getPort() + "\n";
		System.out.println(log);		
		//Files.write(Paths.get(System.getProperty("user.dir") + "\\log.txt")/*Paths.get("~/Desktop/TennisServer/log.txt")*/, log.getBytes(), StandardOpenOption.APPEND);
		Files.write(Paths.get("/home/irina/Desktop/TennisServer/log.txt"), log.getBytes(), StandardOpenOption.APPEND);
	}
	
	@Override
	public void run() 
	{
		while (true)
		{
			try 
			{
				playerSocket = null;
				playerSocket = serverSocket.accept();
				
				Player player = new Player(playerSocket);
				Thread thread = new Thread(player);
				thread.setDaemon(true);
				thread.start();
				playerList.add(player);
				System.out.println("4");
				writeToLog("Client connect:", playerSocket);
				System.out.println("7");
			}
			catch (IOException e) 
			{
			}
		}		
	}
	
	
	private class Player implements Runnable, Serializable
	{
		private static final long serialVersionUID = 1L;
		
		private Socket playerSocket;
		private ObjectInputStream playerInputStream;
		private ObjectOutputStream playerOutputStream;
		
		private boolean isOwnCourt = false;
		
		public Player(Socket playerSocket) throws IOException
		{
			this.playerSocket = playerSocket;
			this.playerInputStream = new ObjectInputStream(playerSocket.getInputStream());
			this.playerOutputStream = new ObjectOutputStream(playerSocket.getOutputStream());
		}
		
		@Override
		public void run() 
		{
			while (true)
			{		
				if (!playerSocket.isClosed())
				{
					
					while (playerSocket.isConnected())
					{
						Integer command = 0;
						try 
						{
							command = (Integer)playerInputStream.readObject();
						} 
						catch (IOException e) 
						{	
							close();
						} 
						catch (ClassNotFoundException e)
						{
							close();
						}
						
						switch (command) 
						{
						case 1:
							try 
							{
								IPList = new String[playerList.size()];
								int i = 0;
								for (Player p : playerList) 
								{
									if (p.isOwnCourt == true)
									{
										IPList[i] = p.playerSocket.getInetAddress().getHostAddress();
										i++;
									}
								}
								writeToLog("Client asket for a list of tables:", playerSocket);
								playerOutputStream.writeObject(IPList);
								playerOutputStream.flush();
								
							} 
							catch (IOException e) {	
								close();
							}
							break;
						case 2:
							try {
								writeToLog("Created a table:", playerSocket);
							} catch (IOException e1) {}
							this.isOwnCourt = true;
							break;
						case 3:
							try 		
							{
								String ip = (String)playerInputStream.readObject();
								
								for (Player p : playerList) 
								{
									if (p.playerSocket.getInetAddress().getHostAddress().equals(ip))
									{
										p.isOwnCourt = false;
										writeToLog("Client start the game:", playerSocket);
										break;
									}
								}
							} 
							catch (ClassNotFoundException | IOException e) {}
							break;
						default:
							break;
						}	
					}
					
					close();
				}
				else
				{
					close();
					break;
				}
			}	
		}
		
		public void close()
		{
			playerList.remove(this);
			if (!this.playerSocket.isClosed())
			{
				try
				{
					this.playerSocket.close();
					writeToLog("Client unconnect:", playerSocket);
				}
				catch (IOException ignored){}
			}
		}	
	}	
}
