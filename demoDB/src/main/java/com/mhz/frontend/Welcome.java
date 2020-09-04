package com.mhz.frontend;
import com.mhz.booking.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class Welcome extends HttpServlet {
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setContentType("text/html");
        PrintWriter pw = res.getWriter();
        req.getRequestDispatcher("link.html").include(req, res);

        HttpSession session = req.getSession(false);
        Query currentQuery = (Query)session.getAttribute("query");
        pw.println("Welcome: " + currentQuery.getCurrentUserName());
        pw.close();
    }
}