package com.mhz.frontend;

import com.mhz.booking.Query;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

public class Search extends HttpServlet {
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setContentType("text/xml;charset=UTF-8");
        PrintWriter pw = res.getWriter();
        pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        String origin = req.getParameter("origin");
        String dest = req.getParameter("dest");
        int dayOfMonth = Integer.parseInt(req.getParameter("day"));
        int numberOfItineraries = Integer.parseInt(req.getParameter("itineraries"));
        boolean indirectFlight = req.getParameter("directOrNot").equals("No");
        HttpSession currentSession = req.getSession();
        if (currentSession.getAttribute("query") == null) { // Not logged in
            Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession(false);
            Query tmpQuery = new Query(session);
            String searchResults = tmpQuery.search(origin, dest, dayOfMonth, numberOfItineraries, indirectFlight);
            pw.print(searchResults);
        } else {
            Query currentQuery = (Query)currentSession.getAttribute("query");
            String searchResults = currentQuery.search(origin, dest, dayOfMonth,
                    numberOfItineraries, indirectFlight);
            pw.print(searchResults);
        }
    }
}
