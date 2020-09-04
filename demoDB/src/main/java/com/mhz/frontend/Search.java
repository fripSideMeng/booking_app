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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Search extends HttpServlet {
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        ArrayList<Integer> id = new ArrayList<>();

        res.setContentType("text/html");
        PrintWriter pw = res.getWriter();

        req.getRequestDispatcher("basic.html").include(req, res);

        pw.println("<!DOCTYPE html>");
        pw.println("<html>");
        pw.print(
                "<head>\n<meta charset='ISO-8859-1'>\n" +
                        "<title>Search Page</title><style>\nh1 {text-align:center;}\n" +
                        "p {text-align: center;}\n</style>\n</head>\n");

        String origin = req.getParameter("origin");
        String dest = req.getParameter("dest");
        int dayOfMonth = Integer.parseInt(req.getParameter("day"));
        int numberOfItineraries = Integer.parseInt(req.getParameter("itineraries"));
        boolean indirectFlight = req.getParameter("directOrNot").equals("no");
        HttpSession currentSession = req.getSession();
        if (currentSession.getAttribute("query") == null) { // Not logged in
            Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession(false);
            Query tmpQuery = new Query(session);
            String searchResults = tmpQuery.search(origin, dest, dayOfMonth, numberOfItineraries, indirectFlight);
            pw.print("<body>\n" + searchResults
                    + "<a href='index.html'>Please Log in to search and book</a>\n</body>\n</html>");
            pw.close();
        } else {

        }
    }
}
