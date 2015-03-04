package io.github.javaconductor.gserv.plugins.ldap

import groovy.util.logging.Slf4j
import io.github.javaconductor.gserv.requesthandler.RequestContext
import io.github.javaconductor.gserv.delegates.DelegateTypes
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback
import org.springframework.ldap.core.LdapEntryIdentification
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import io.github.javaconductor.gserv.plugins.AbstractPlugin

import javax.naming.directory.DirContext

@Slf4j
class LdapPlugin extends AbstractPlugin {
    LdapTemplate ldapTemplate
    String challengeFnName

    @Override
    Object init(Object options) {
        options = options ?: [:]

        /// create the template
        LdapContextSource ctxSrc = new LdapContextSource();
        ctxSrc.setUrl(options.url);
        ctxSrc.setBase(options.base);
        ctxSrc.setUserDn(options.userDn);
        ctxSrc.setPassword(options.password);
        challengeFnName = options.authenticationFunctionName ?: "ldapAuthentication"
        ctxSrc.afterPropertiesSet(); // this method should be called.

        ldapTemplate = new LdapTemplate(ctxSrc);
    }

    boolean authenticate(String user, String pswd, RequestContext requestContext, Closure dnCallback) {
        def authenticated
        try {
            authenticated = ldapTemplate.authenticate(
                    "",
                    "(uid=$user)",
                    "$pswd", new AuthenticatedLdapEntryContextCallback() {
                @Override
                void executeWithContext(DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                    println "auth: absoluteName: ${ldapEntryIdentification.absoluteName}"
                    println "auth: relativeName: ${ldapEntryIdentification.relativeName}";
                    if (dnCallback)
                        dnCallback(ldapEntryIdentification.absoluteName.toString())
                }

            })
        } catch (org.springframework.ldap.CommunicationException e) {

            error(403, "Cannot authenticate at this time.")
            log.error("Cannot connect to LDAP Server.", e?.cause ?: e)
        }
        authenticated
    }//auth

    @Override
    MetaClass decorateDelegate(String delegateType, MetaClass delegateMetaClass) {

        if (delegateType == DelegateTypes.Http || delegateType == DelegateTypes.Https) {

            delegateMetaClass.ldapAuthentication << { user, pswd, requestContext, dnCallback ->
                authenticate(user, pswd, requestContext, dnCallback)
            }
            delegateMetaClass.ldapAuthentication << { user, pswd, requestContext ->
                authenticate(user, pswd, requestContext, null)
            }
        }
        delegateMetaClass
    }
}
