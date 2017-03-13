package leaderboard;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

import encryption.Encryption;

public class LeaderboardClient {
	private static final Base64.Encoder BASE64ENCODER = Base64.getEncoder();
	private static final Base64.Decoder BASE64DECODER = Base64.getDecoder();
	private static LeaderboardClient INSTANCE = new LeaderboardClient();
	private static String SERVERADDRESS;
	private static int PORT;
	
	public static final int LB_SPACEINVADERS = 0;
	public static final int LB_SNAKE = 1;
	public static final int LB_PONG = 2;
	public static final int LB_BRICKBREAKER = 3;
	public static final int LB_LEADERBOARDREQUEST = 4;
	
	/**
	 * 
	 * @return
	 */
	public static LeaderboardClient getInstance() {
		return INSTANCE;
	}
	
	/**
	 * 
	 */
	public LeaderboardClient() {
		if(runners.Main.DEBUGGINGMODE){
         SERVERADDRESS = "localhost";
         }
         else{
         SERVERADDRESS = "73.254.70.133";
         }
		PORT = 20090;
	}
	
	public String[] requestLeaderboardData() {
		return getLeaderboards(Encryption.getInstance().encrypt(LB_LEADERBOARDREQUEST + ""));
	}
	
	/**
	 * 
	 * @param score
	 * @return
	 */
	public String[] spaceInvadersRequestLeaderboards(String score) {
		return getLeaderboards(Encryption.getInstance().encrypt(LB_SPACEINVADERS+score));
	}
	
	/**
	 * 
	 * @param score
	 * @return
	 */
	public String[] snakeRequestLeaderboards(String score) {
		return getLeaderboards(Encryption.getInstance().encrypt(LB_SNAKE+score));
	}
	
	/**
	 * 
	 * @param score
	 * @return
	 */
	public String[] pongRequestLeaderboards(String score) {
		return getLeaderboards(Encryption.getInstance().encrypt(LB_PONG+score));
	}
	
	/**
	 * 
	 * @param score
	 * @return
	 */
	public String[] brickBreakerRequestLeaderboards(String score) {
		return getLeaderboards(Encryption.getInstance().encrypt(LB_BRICKBREAKER+score));
	}
	
	private int tries = 0;
	/**
	 * 
	 * @param score Requires that LB_SPACEINVADERS, LB_SNAKE, LB_PONG, or LB_BRICKBREAKER 
	 * 		is added to the beginning of the score array before passing it to this method as encryptedData.
	 * @return 
	 */
	private String[] getLeaderboards(byte[] score) {
		tries = 0;
		String[] leaderboard = new String[0];
		while(tries < 5 && leaderboard.length < 1) {
			tries++;
			try {
				Socket socket = new Socket(SERVERADDRESS,PORT);
				socket.setKeepAlive(true);
				socket.setTcpNoDelay(true);
				socket.setSoTimeout(10000);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
				String leaderboardData = "";
				out.println(BASE64ENCODER.encodeToString(score));
				leaderboardData = Encryption.getInstance().decrypt(BASE64DECODER.decode(in.readLine()));
				leaderboard = leaderboardData.split(",");
				socket.close();
			} catch(Exception e) {
				if(tries == 5) {
					e.printStackTrace();
				}
			}
		}
		return leaderboard;
	}
	
	public static void main(String[] args) {
		LeaderboardClient leaderboardClient = LeaderboardClient.getInstance();
		int requests = 1;
		long startTime = System.currentTimeMillis();
		for(int i = 0; i < requests; i++) {
			System.out.println((i+1) + ": " + Arrays.toString(leaderboardClient.brickBreakerRequestLeaderboards("AAA 000")));
		}
		System.out.println("Time to query @" + requests + " was: " + (System.currentTimeMillis() - startTime));
	}
}
/**
 * AAA_000,BBB_111,CCC_222
 * 01234567890123456789012
 * 00000000001111111111222
 * 
 */