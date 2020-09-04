package com.mhz.frontend;
import com.mhz.booking.Query;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.TransactionIsolationLevel;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

public class Login implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req;
        req = (HttpServletRequest) request;
        HttpServletResponse res;
        res = (HttpServletResponse) response;

        res.setContentType("text/html");
        PrintWriter pw = res.getWriter();
        Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        SqlSession session = sqlSessionFactory.openSession(TransactionIsolationLevel.SERIALIZABLE);

        if (req.getParameter("userName") != null && req.getParameter("userPass") != null) {
            String nameInput = req.getParameter("userName");
            String passwd = req.getParameter("userPass");
            Query currentQuery = new Query(session);
            String logInfo = currentQuery.login(nameInput, passwd, 4);
            if (logInfo.startsWith("Logged in as")) {
                HttpSession currentSession = req.getSession(true);
                currentSession.setAttribute("query", currentQuery);
                chain.doFilter(req, res);
            } else {
                pw.println(logInfo);
                RequestDispatcher rd = req.getRequestDispatcher("index.html");
                rd.include(req, res);
            }
        } else if (req.getSession(false) != null
                && req.getSession(false).getAttribute("query") != null) {
            chain.doFilter(req, res);
        }
        pw.close();
    }
}
