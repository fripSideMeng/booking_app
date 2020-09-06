package com.mhz.frontend;
import com.mhz.booking.Query;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class ViewReservation extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        HttpSession currentSession = req.getSession(false);
        if (currentSession == null
            || currentSession.getAttribute("query") == null
            || ((Query)currentSession.getAttribute("query"))
                .getCurrentUserName().equals("Not logged in")) {
            RequestDispatcher rd = req.getRequestDispatcher("index.html");
            rd.include(req, res);
        } else {
            Query currentQuery = (Query)currentSession.getAttribute("query");
            res.setContentType("text/xml;charset=UTF-8");
            PrintWriter pw = res.getWriter();
            String reservations = currentQuery.view_reservations(currentQuery.getCurrentUserName(), 4);
            pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            pw.print("<Reservations>\n");
            pw.print(reservations);
            pw.println("</Reservations>");
        }
    }
}
