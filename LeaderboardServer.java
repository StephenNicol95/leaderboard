package leaderboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;

import encryption.Encryption;

public class LeaderboardServer implements Runnable {
	public static Base64.Encoder BASE64ENCODER = Base64.getEncoder();
	public static Base64.Decoder BASE64DECODER = Base64.getDecoder();
	private static ArrayList<Thread> THREADS = new ArrayList<Thread>();
	private static ArrayList<Socket> SOCKETS = new ArrayList<Socket>();
	private static String[] fileNames = { "SpaceInvaders.dat",
									"Snake.dat",
									"Pong.dat",
									"BrickBreaker.dat" };
	private static File[] LEADERBOARDFILES;
	private static String[][] LEADERBOARD;
	private int DEFAULTPORT;
	//private static int RECENT;
	
	/**
	 * Initializes the Server object's variables and selects the port to run on.
	 * @param DEFAULTPORT Port on which to run the SocketServer
	 */
	public LeaderboardServer(int DEFAULTPORT) {
		this.DEFAULTPORT = DEFAULTPORT;
		LEADERBOARD = new String[4][3];
		//RECENT = -1;
		LEADERBOARDFILES = new File[4];
		try {
			Scanner scan;
			int lineNumb = 0;
			//initialize leaderboard
			for(int i = 0; i < LEADERBOARD.length; i++) {
				for(int j = 0; j < LEADERBOARD[0].length; j++) {
					LEADERBOARD[i][j] = "AAA 000";
				}
			}
			
			//load data from file, in case the initial leaderboard has actually been updated.
			for(int i = 0; i < LEADERBOARDFILES.length; i++) {
				lineNumb = 0;
				if(!(LEADERBOARDFILES[i] = new File(fileNames[i])).exists()) {
					LEADERBOARDFILES[i].createNewFile();
				} else {
					scan = new Scanner(LEADERBOARDFILES[i]);
					while(scan.hasNextLine()) {
						LEADERBOARD[i][lineNumb] = scan.nextLine();
						lineNumb++;
					}
				}
			}
			updateData();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts the server, on the same thread.
	 */
	@SuppressWarnings("resource")
	public void startServer() {
		System.out.println("Initializing Server");
		try {
			ServerSocket socketServer = new ServerSocket(DEFAULTPORT);
			System.out.println("Accepting Connections");
			while(true) {
				Socket socket = socketServer.accept();
				System.out.println(socket.getInetAddress().getHostAddress() + " has requested data for a game.");
				SOCKETS.add(socket);
				//RECENT = SOCKETS.size();
				Thread clientThread = new Thread(new LeaderboardServer(-1));
				THREADS.add(clientThread);
				clientThread.start();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the port on which to run - CALL BEFORE STARTSERVER
	 * @param DEFAULTPORT The port you wish to run the server on.
	 */
	public void setPort(int DEFAULTPORT) {
		this.DEFAULTPORT = DEFAULTPORT;
	}
	
	private void updateData() {
		FileWriter fw;
		try {
			for(int i = 0; i < LEADERBOARD.length; i++) {
				if(LEADERBOARDFILES[i] == null) {
					LEADERBOARDFILES[i] = new File(fileNames[i]);
				}
				fw = new FileWriter(LEADERBOARDFILES[i]);
				for(int j = 0; j < LEADERBOARD[i].length; j++) {
					fw.write(LEADERBOARD[i][j] + "\n");
				}
				fw.flush();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This is a client-handler
	 */
	@Override
	public void run() {
		Socket socket = SOCKETS.get(SOCKETS.size()-1);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
			String data = Encryption.getInstance().decrypt(BASE64DECODER.decode(br.readLine().getBytes()));
			int toCheck = Integer.parseInt(data.substring(0,1));
			String toSend = "";
			
			//Check if it's just checkLeaderboard or actual game data:
			if(toCheck < 4 && toCheck > -1)
				toSend = checkLeaderboard(toCheck,data.substring(1));
			else
				toSend = sendLeaderboard();
			
			//Send the data back, encrypted of course
			pw.println(new String(BASE64ENCODER.encode(Encryption.getInstance().encrypt(toSend))));
			socket.close();
			System.gc();
		} catch(Exception e) {
			if(!e.getMessage().toLowerCase().contains("socket is closed"))
				e.printStackTrace();
		}
	}
	
	private synchronized String sendLeaderboard() {
		String toReturn = "";
		for(int i = 0; i < LEADERBOARD.length; i++) {
			for(int j = 0; j < LEADERBOARD[0].length; j++) {
				toReturn += LEADERBOARD[i][j] + (i == LEADERBOARD.length && j == LEADERBOARD[0].length ? "" : ", ");
			}
		}
		return toReturn;
	}
	
	private synchronized String checkLeaderboard(int game, String data) {
		String toSend = "";
		for(int i = 0; i < LEADERBOARD[game].length; i++) {
			if(Integer.parseInt(LEADERBOARD[game][i].substring(4)) < Integer.parseInt(data.substring(4))) {
				if(i == 0) {
					LEADERBOARD[game][2] = LEADERBOARD[game][1];
					LEADERBOARD[game][1] = LEADERBOARD[game][0];
				} else if(i == 1) {
					LEADERBOARD[game][2] = LEADERBOARD[game][1];
				}
				LEADERBOARD[game][i] = data;
				data = data.substring(0, 4) + "0";
			}
		}
		toSend = LEADERBOARD[game][0] + "," + LEADERBOARD[game][1] + "," + LEADERBOARD[game][2];
		updateData();
		return toSend;
	}
	
	public static void main(String[] args) {
		LeaderboardServer leaderboardServer = new LeaderboardServer(20090);
		leaderboardServer.startServer();
	}
}

/************* Data pertaining to the way LeaderboardData is handled **************/
/** 		 /<-- Client 1
 *          |
 * Server --|<--- Client 2
 * 			|
 * 			 \<-- Client 3
 * 
 * ^ Scores sent to server
 * 
 *			 /--- Client 1
 *          |
 * Server->-| --- Client 2
 * 			|
 * 			 \--- Client 3
 *  
 * ^ Leaderboard received from server
 * 
 * Client 1 sends data, Client 1 receives Leaderboard data
 * Client 2 sends data, Client 2 receives Leaderboard data
 * Client 3 sends data, Client 3 receives Leaderboard data
 * (per-thread 'synchronize' basis for checking for the top-3 users)
 * 
 * Client data handle:
 * 	-Stores top 3 scores in RAM & on the Local Drive
 *  -score works like this:
 * 		>["BBB" + " "][int] is sent to the server -> int can be any integer number
 *  	-server stores (encrypted, to the Local Drive):
 *  		"BBB 444"
 * 			 0123456
 * 
 * Game List:
 *  0. Space Invaders
 *  1. Snake
 *  2. Pong
 *  3. Brick Breaker
 */