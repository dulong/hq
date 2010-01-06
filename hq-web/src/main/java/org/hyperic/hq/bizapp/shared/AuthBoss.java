/*
 * Generated by XDoclet - Do not edit!
 */
package org.hyperic.hq.bizapp.shared;

import javax.security.auth.login.LoginException;

import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.util.ConfigPropertyException;

/**
 * Local interface for AuthBoss.
 */
public interface AuthBoss {
    /**
     * Add buffered listener to register login audits post commit. This allows
     * for read-only operations to succeed properly when accessed via HQU
     */
    public void startup();

    /**
     * Login a user.
     * @param username The name of the user.
     * @param password The password.
     * @return An integer representing the session ID of the logged-in user.
     */
    public int login(String username, String password) throws SecurityException, LoginException, ApplicationException,
        ConfigPropertyException;

    /**
     * Login a guest.
     * @return An integer representing the session ID of the logged-in user.
     */
    public int loginGuest() throws SecurityException, LoginException, ApplicationException, ConfigPropertyException;

    /**
     * Logout a user.
     * @param sessionID The session id for the current user
     */
    public void logout(int sessionID);

    /**
     * Check if a user is logged in.
     * @param username The name of the user.
     * @return a boolean| true if logged in and false if not.
     */
    public boolean isLoggedIn(String username);

    /**
     * Add a user to the internal database
     * @param sessionID The session id for the current user
     * @param username The username to add
     * @param password The password for this user
     */
    public void addUser(int sessionID, String username, String password) throws SessionException;

    /**
     * Change a password for a user
     * @param sessionID The session id for the current user
     * @param username The user whose password should be updated
     * @param password The new password for the user
     */
    public void changePassword(int sessionID, String username, String password) throws PermissionException,
        SessionException;

    /**
     * Check existence of a user
     */
    public boolean isUser(int sessionID, String username) throws SessionTimeoutException, SessionNotFoundException;

}
