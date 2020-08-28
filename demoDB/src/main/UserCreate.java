import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.*;
import java.sql.*;
import javax.naming.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class UserCreate extends HttpServlet {

	private Context ctx;
	private DataSource ds;
	private static final String USER_CREATE="INSERT INTO Users VALUES(?, ?, ?, ?)";
	private static final int HASH_STRENGTH = 65536;
	private static final int KEY_LENGTH = 128;

	public void init() throws ServletException {
		try {
			ctx = new InitialContext();
			ds = (DataSource)ctx.lookup("java:comp/env/jdbc/FlightDB");		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		Connection conn = null;
		PreparedStatement userCreateStatement = null;
		try {
			conn = ds.getConnection();
			conn.setAutoCommit(true);
			conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
			userCreateStatement = conn.prepareStatement(USER_CREATE);
		} catch (SQLException e) {
			pw.println("Internal error, please retry");
			req.getRequestDispatcher("index.html").include(req, res);
			try {
				conn.close();
			} catch (SQLException e1) {
			
			}
			return;
		}

		String name = req.getParameter("userName");
		String passwd = req.getParameter("userPass");

		if (name == null || passwd == null || name.contains(";") || name.contains(" ") || name.contains(",") || name.length() > 20) {
			pw.println("Username format error. Username length cannot exceed 20 and cannot contain semi-colon, comma, underline or space");
			req.getRequestDispatcher("createUser.html").include(req, res);
			return;
		}

		int initAmount = 0;
		try {
			initAmount = Integer.parseInt(req.getParameter("deposit"));
			if(initAmount < 5000) {
				pw.println("Cannot set deposit, amount should be at least 5000");
				req.getRequestDispatcher("createUser.html").include(req, res);
				return;
			}
		} catch (Exception e) {
			pw.println("Cannot set deposit, only support integer value under " + Integer.MAX_VALUE);
			req.getRequestDispatcher("createUser.html").include(req, res);
			return;
		}

		int retry = 0;
		String status = transaction_createCustomer(name, passwd, initAmount, userCreateStatement);

		if(!status.equals("Failed to create user\n")) {
			pw.println("Created user " + name);
			req.getRequestDispatcher("index.html").include(req, res);
		} else {
			if (retry < 3) {
				retry++;
				pw.println("Failed to create user " + name);
				req.getRequestDispatcher("createUser.html").include(req, res);
				return;

			} else {
				pw.println("Failed to create, try again later...");
				req.getRequestDispatcher("index.html").include(req, res);
				return;
			}
		}
		
		pw.close();
		try {
			conn.close();
		} catch (SQLException e) {
			
		}
	}

	public String transaction_createCustomer(String username, String password, int initAmount, PreparedStatement userCreateStatement) {
		try {
			if (initAmount < 0) {
				return "Failed to create user\n";
			}
			// Generate a random cryptographic salt
			SecureRandom random = new SecureRandom();
			byte[] salt = new byte[16];
			random.nextBytes(salt);

			// Specify the hash parameters
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);

			// Generate the hash
			SecretKeyFactory factory = null;
			byte[] hash = null;
			try {
				factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
				hash = factory.generateSecret(spec).getEncoded();
			} catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
				throw new IllegalStateException();
			}
			userCreateStatement.setString(1, username);
			userCreateStatement.setBytes(2, hash);
			userCreateStatement.setBytes(3, salt);
			userCreateStatement.setInt(4, initAmount);
			int count = userCreateStatement.executeUpdate();
			if (count == 1) {
				return "Created user " + username + "\n";
			}
			return "Failed to create user\n";

		} catch (SQLException e) {
			return "Failed to create user\n";
		} 
	}	

}
