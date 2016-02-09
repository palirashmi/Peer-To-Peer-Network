import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.io.*;
/*import java.nio.*;
import java.nio.channels.*;
import java.util.*;
 */
public class Server {

	private static final int sPort = 8000;   //The server will be listening on this port number
	private static File file ;
	private static int noOfChunks;
	private static int chunkSize=100000;
	private final static int noOfpeer=5;
	static int noOfChunksToSendToeach;
	static int noOfExtraChunk;
	static int sentChunks=1;
	static int counterOfSentChunks=1;
	
	public void initialize()
	{
		System.out.println("The server is running.");
		System.out.println("Input the name of file to split");
		Scanner scanner = new Scanner(System.in);
		file=new File(scanner.next());
		
		System.out.println("Splitting given file into pieces.");
		try {
			splitFile(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		noOfChunks=(int) Math.ceil(file.length()/100000.0);
		System.out.println("File succesfully Split into pieces. ");
		System.out.println("No of chunks : "+noOfChunks+"  File size "+file.length());
		noOfChunksToSendToeach=noOfChunks/noOfpeer;
		noOfExtraChunk=noOfChunks%noOfpeer;
		System.out.println(noOfChunksToSendToeach+" "+noOfChunks+"  "+noOfExtraChunk);
	}
	
	public static void main(String[] args) throws Exception {
		
		Server server = new Server();
		server.initialize();
		ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;
		try {
			while(true) {
				new Handler(listener.accept(),clientNum).start();
				System.out.println("Client "  + clientNum + " is connected!");
				clientNum++;
			}
		} finally {
			listener.close();
		} 

	}

	/**
	 * A handler thread class.  Handlers are spawned from the listening
	 * loop and are responsible for dealing with a single client's requests.
	 */
	private static class Handler extends Thread {
		private String message;    //message received from the client
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int counter;			//The index number of the client
		 
		
		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.counter = no;
		}

		public void run() {
			try{
				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				
				try{
					while(true)
					{
						message = (String)in.readObject();
						System.out.println("Received message: " + message + " from client " + counter);
						if(counter==noOfpeer){
							sentChunks=noOfChunksToSendToeach+noOfExtraChunk;
						}else{
							sentChunks=noOfChunksToSendToeach;
						}
						
						out.writeInt(sentChunks);
						out.writeInt(noOfChunks);
						for(int i=1;i<=sentChunks;i++){
							out.writeInt(counterOfSentChunks);
							String tempfile= new String();
							tempfile=file+"."+counterOfSentChunks;
							File inputFile = new File(tempfile);	
							System.out.println("Sending chunk "+counterOfSentChunks +"file to client "+counter);
							sendMessage(inputFile);	
							counterOfSentChunks++;
							
						}
					}
				}
				catch(ClassNotFoundException classnot){
					System.err.println("Data received in unknown format");
				}
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + counter);
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + counter);
				}
			}
		}

		//send a message to the output stream
		public void sendMessage(File partOfFile)
		{
			try{
				out.writeObject(partOfFile);
				out.flush();
				System.out.println("Sending :: " + partOfFile + " to Client " + counter);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

	}

	public  void splitFile(File f) throws IOException {
		int partCounter = 1;
		int sizeOfFiles = 100000;// 100KB100*1000
		byte[] buffer = new byte[sizeOfFiles];
		try (BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(f))) {//try-with-resources to ensure closing stream
			String name = f.getName();
			int tmp = 0;
			while ((tmp = bis.read(buffer)) > 0) {
				File newFile = new File(f.getParent(), name + "."
						+ String.format("%01d", partCounter++));
				try (FileOutputStream out = new FileOutputStream(newFile)) {
					out.write(buffer, 0, tmp);
				}
			}
		}
	}
}
