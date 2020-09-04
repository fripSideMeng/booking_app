package com.mhz.frontend;
import com.mhz.booking.Query;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Logout extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setContentType("text/html");
        PrintWriter pw = res.getWriter();
        req.getRequestDispatcher("link_out.html").include(req, res);

        HttpSession currentSession = req.getSession(false);
        if (currentSession.getAttribute("query") != null) {
            Query currentQuery = (Query)currentSession.getAttribute("query");
            currentQuery.closeConnection();
        }
        currentSession.invalidate();
        pw.println("Logout successfully...");
        pw.close();
    }
}
