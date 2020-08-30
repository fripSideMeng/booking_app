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

public class Login implements Filter {

	private static final String USER_LOG_IN = "SELECT Password, Salt FROM Users WHERE Username = ?";
	private Context ctx;
	private DataSource ds;
	private static final int HASH_STRENGTH = 65536;
	private static final int KEY_LENGTH = 128;

	public void init(FilterConfig arg0) throws ServletException {

		try {
			ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:comp/env/jdbc/FlightDB");
		} catch (Exception e) {
		
		}

	};

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		PreparedStatement userLogInStatement;
		
		HttpServletRequest req;
		req = HttpServletRequest.class.cast(request);
		HttpServletResponse res;
		res = HttpServletResponse.class.cast(response);

		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		Connection conn;
		try {
			conn = ds.getConnection();
			conn.setAutoCommit(true);
			conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
			userLogInStatement = conn.prepareStatement(USER_LOG_IN);
		} catch (Exception e) {
			pw.println("Connection problem, retry later");
			req.getRequestDispatcher("index.html").include(req, res);
			return;
		}
		
		String nameInput = "";
		String passwd = "";

		if (req.getParameter("userName") != null && req.getParameter("userPass") != null) {
			nameInput = req.getParameter("userName");
			passwd = req.getParameter("userPass");
		} else if (req.getSession(false) != null && req.getSession(false).getAttribute("uname") != null) {
			chain.doFilter(req, res);
		}

		String status = transaction_login(nameInput, passwd, userLogInStatement);
		if (!status.equals("Login failed\n")) {
			HttpSession session = req.getSession();
			synchronized (session) {
				session.setAttribute("conn", conn);
				session.setAttribute("uname", nameInput);
			}
			chain.doFilter(req, res);
		} else {
			pw.println("Username or password error!");
			RequestDispatcher rd = req.getRequestDispatcher("index.html");
			rd.include(req, res);
		}
		pw.close();
	}

	public String transaction_login(String username, String password, PreparedStatement userLogInStatement) {
		try {
			// Get the cryptographic salt in db
			userLogInStatement.setString(1, username);
			ResultSet rs = userLogInStatement.executeQuery();
			byte[] hash_db = null;
			byte[] salt = null;

			if (rs.next()) {
				try {
					hash_db = rs.getBytes("Password");
					salt = rs.getBytes("Salt");
				} catch (NullPointerException e) {
					return "Login failed\n";
				}
			} else {
				return "Login failed\n";
			}

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

			rs.close();
			if (Arrays.equals(hash, hash_db)) {
				return "Logged in as " + username + "\n";
			} else {
				return "Login failed\n";
			}

		} catch (SQLException e) {
			return "Login failed\n";
		}
	}

}
