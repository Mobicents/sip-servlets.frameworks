package org.mobicents.servlet.sip.weld.examples;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;

import org.apache.log4j.Logger;


public class SimpleWebServlet extends HttpServlet
{ 	
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(SimpleWebServlet.class);
	
	@Inject
	private SipFactory sipFactory;	
	
    /**
     * Handle the HTTP GET method by building a simple web page.
     */
    public void doGet (HttpServletRequest request,
            	   HttpServletResponse response)
    throws ServletException, IOException
    {
        String toAddr = request.getParameter("to");
        String fromAddr = request.getParameter("from");
        String bye = request.getParameter("bye");

        URI to = toAddr==null? null : sipFactory.createAddress(toAddr).getURI();
        URI from = fromAddr==null? null : sipFactory.createAddress(fromAddr).getURI();    

        CallStatusContainer calls = (CallStatusContainer) getServletContext().getAttribute("activeCalls");

        // Create app session and request
        SipApplicationSession appSession = 
        	((ConvergedHttpSession)request.getSession()).getApplicationSession();

        if(bye != null) {
        	if(bye.equals("all")) {
        		Iterator it = (Iterator) appSession.getSessions("sip");
        		while(it.hasNext()) {
        			SipSession session = (SipSession) it.next();
        			Call call = (Call) session.getAttribute("call");
        			call.end();
        			calls.removeCall(call);
        		}
        	} else {
        		// Someone wants to end an established call, send byes and clean up
        		Call call = calls.getCall(fromAddr, toAddr);
        		call.end();
        		calls.removeCall(call);
        	}
        } else {
        	if(calls == null) {
        		calls = new CallStatusContainer();
        		getServletContext().setAttribute("activeCalls", calls);
        	}
        	
        	// Add the call in the active calls
        	Call call = calls.addCall(fromAddr, toAddr, "FFFF00");

        	SipServletRequest req = sipFactory.createRequest(appSession, "INVITE", from, to);

        	// Set some attribute
        	req.getSession().setAttribute("SecondPartyAddress", sipFactory.createAddress(fromAddr));
        	req.getSession().setAttribute("call", call);
        	
        	// This session will be used to send BYE
        	call.addSession(req.getSession());
        	
        	logger.info("Sending request" + req);
        	// Send the INVITE request            
        	req.send();
        }
        
        // Write the output html
    	PrintWriter	out;
        response.setContentType("text/html");
        out = response.getWriter();
        
        // Just redirect to the index
        out.println("<HTML><META HTTP-EQUIV=\"Refresh\"CONTENT=\"0; URL=index.jsp\"><HEAD><TITLE></HTML>");
        out.close();
    }
}