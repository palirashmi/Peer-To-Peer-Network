//package Client1;



import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.*;

/*import java.nio.*;
import java.nio.channels.*;
import java.util.*;
 */
public class Client {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	static int SeverPort=8000;
	static int clientId=2;
	int downloadNeighbour=3;
	int uploadNeigbour=1;
	private Socket requestChunkSocket;
	static int totalNoOfChunks;
	static Set<Integer> set = new HashSet<Integer>();
	static String filename; 
	ObjectOutputStream outClient;         
	ObjectInputStream inClient;          
	ObjectInputStream inClientNum; 
	static boolean neighbourReceivedAllChunks=false;


	public void receiveFromServer()
	{
		try{
			//create a socket to connect to the server
			requestSocket = new Socket("localhost", SeverPort);
			System.out.println("Connected to localhost in port "+SeverPort);
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());

			sendMessage("Request for chunks of files");
			int numOfChunks=in.readInt();
			totalNoOfChunks=in.readInt();
			int chunkNo;

			System.out.println(numOfChunks);
			int i=1;
			System.out.println(numOfChunks);
			do {
				i++;
				chunkNo=in.readInt();
				System.out.println(chunkNo+ " chunkNo received");
				FileOutputStream fileOutput;
				FileInputStream fileInput;
				File sentFile= (File)in.readObject();

				System.out.println("File chunk named "+sentFile.getName() +"+of size"+sentFile.length()+" received");
				set.add(chunkNo);
				fileInput=new FileInputStream(sentFile);
				byte [] fileBytes = new byte[(int) sentFile.length()];
				int bytesRead = fileInput.read(fileBytes, 0,(int)  sentFile.length());
				retrieveAndSetFilename(sentFile.getName());
				fileOutput = new FileOutputStream(new File("client"+clientId+"."+sentFile.getName()));
				fileOutput.write(fileBytes);
				fileOutput.flush();
				fileOutput.close();


			} while (i<=numOfChunks);

		}
		catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
			System.err.println("Class not found");
		} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{

				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	public void retrieveAndSetFilename(String string){
		String tmpName = string;//{name}.{number}
		String destFileName = tmpName.substring(0, tmpName.lastIndexOf('.'));
		filename=destFileName;

	}
	public void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	public void sendMessage(Set<Integer> msg)
	{
		try{
			//stream write the message
			outClient.writeObject(msg);
			outClient.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	Boolean getFromNeighbour(){
		System.out.println("Download attempt from peer neighbour");
		try{
			int portNo=SeverPort+downloadNeighbour;
			requestChunkSocket = new Socket("localhost",portNo);

			outClient = new ObjectOutputStream(requestChunkSocket.getOutputStream());
			outClient.flush();

			sendMessage(set);
			inClient = new ObjectInputStream(requestChunkSocket.getInputStream());
			int chunksToArrive=inClient.readInt();
			FileOutputStream fileOutput;
			for(int i=1;i<=chunksToArrive;i++)
			{ 
				FileInputStream fis;
				File sentFile= (File)inClient.readObject();
				fis=new FileInputStream(sentFile);
				byte [] fileBytes = new byte[(int) sentFile.length()];
				int bytesRead = fis.read(fileBytes, 0,(int)  sentFile.length());
				System.out.println(sentFile.getName()+"==received file");
				String recFile=sentFile.getName();
				String temp=recFile.substring(recFile.indexOf(".")+1, recFile.length());
				fileOutput = new FileOutputStream(new File("client"+clientId+"."+temp));
				//fileOutput = new FileOutputStream(new File("client"+clientId+sentFile.getName()));
				fileOutput.write(fileBytes);
				fileOutput.flush();
				fileOutput.close();
				//show the message to the user
				System.out.println("Received File from Neighbour " + sentFile.getName());
				String fileName = sentFile.getName();
				String s=fileName.substring(fileName.lastIndexOf(".")+1,fileName.length() );
				int id = Integer.parseInt(s);
				set.add(id);

			}
			if(allChunkReceived()){
				System.out.println("All chunks to recreate file  have been received");
				try {
					mergeFiles(listOfFilesForMerge(totalNoOfChunks), new File(clientId+"merged.pdf"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("File recreated");
			}
		}
		catch (ConnectException e) {
			System.err.println("No response from Download Peer");
			return false;
		}  
		catch ( ClassNotFoundException e ) {
			System.err.println("Class not found");
		}
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}

		return true;

	}


	public void uploadForOtherNeighbours() {
		try{
			ServerSocket listener = new ServerSocket(SeverPort+clientId);
			System.out.println("Starting listener ");
			while(!neighbourReceivedAllChunks || !allChunkReceived()) {
				getFromNeighbour();
				new Uploader(listener.accept()).start();
				System.out.println("Peer client connected!");
			}
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}		
	}

	public static void mergeFiles(List<File> files, File into)
			throws IOException {
		try (BufferedOutputStream mergingStream = new BufferedOutputStream(
				new FileOutputStream(into))) {
			for (File f : files) {
				Files.copy(f.toPath(), mergingStream);
				mergingStream.flush();
			}
		}
	}
	public static List<File> listOfFilesForMerge( int noOfFiles) {

		/*String tmpName = mainFile.getName();//{name}.{number}

		String destFileName = tmpName.substring(0, tmpName.lastIndexOf('.'));//remove .{number}
		 */
		List<File> files=new ArrayList<File>();
		for (int j = 1; j <=noOfFiles ; j++) {
			//files.add(new File(filename+"."+j));
			files.add(new File("client"+clientId+"."+filename+"."+j));
		}
		System.out.println("List of files to merge to regenrate original file:");
		for (Iterator iterator = files.iterator(); iterator.hasNext();) {
			File file = (File) iterator.next();
			System.out.println(file.getName());

		}

		return files;
	}
	public boolean allChunkReceived(){
		return (set.size()==totalNoOfChunks);
	}
	public static void main(String args[])
	{
		Client client = new Client();
		client.receiveFromServer();
		for (Iterator iterator = set.iterator(); iterator.hasNext();) {
			Integer integer = (Integer) iterator.next();
			System.out.println(integer+"  set ");
		}
		System.out.println(filename);
		client.uploadForOtherNeighbours();
		if(client.allChunkReceived()){
			System.out.println("All chunks to recreate file  have been received");
			try {
				mergeFiles(listOfFilesForMerge(totalNoOfChunks), new File(clientId+"merged.pdf"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("File recreated");
		}



	}
	private static class Uploader extends Thread {

		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out; 


		public Uploader(Socket connect) {
			connection=connect;
		}
		public void run() {

			Set<Integer> setFromNeighbour = new HashSet<Integer>();
			try{ 
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());

				setFromNeighbour = (HashSet<Integer>)in.readObject();
				System.out.println("CHUNK LIST RECIEVED from upload neighbour  " +setFromNeighbour);
				int count=0;
				for(int i=1;i<=totalNoOfChunks;i++){

					if(!setFromNeighbour.contains(i) && set.contains(i)){
						count++;	
					}
				}
				if(count>0) 
				{	neighbourReceivedAllChunks=true;

				}
				System.out.println("Sending out "+count+"  number of chunks to neighbour :");
				out.writeInt(count);
				out.flush();
				for(int i=1;i<=totalNoOfChunks && count>0;i++){
					if(!setFromNeighbour.contains(i) && set.contains(i)){
						String tempfile="client"+clientId+"."+filename+"."+i;
						File inputFile = new File(tempfile);
						sendMessage(inputFile);	
						System.out.println("Chunk"+i+"Sent");
					}
				}

			}
			catch(IOException ioException){
				System.out.println("Disconnect with Neighbour Client");
			}
			catch(ClassNotFoundException classnot){
				System.err.println("Data received in unknown format");
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client");
				}
			}
		}

		public void sendMessage(File msg)
		{
			try{
				out.writeObject(msg);
				out.flush();
				System.out.println("Sent message: " + msg + " to UPLOAD Peer");
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

	}


}


