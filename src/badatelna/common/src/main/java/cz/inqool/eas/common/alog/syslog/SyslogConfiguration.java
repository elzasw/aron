package cz.inqool.eas.common.alog.syslog;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.sender.AbstractSyslogMessageSender;
import com.cloudbees.syslog.sender.SyslogMessageSender;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for syslog subsystem.
 *
 * If application wants to use syslog subsystem,
 * it needs to extend this class and add {@link Configuration} annotation.
 *
 */
@EnableAsync
@EnableRetry
public abstract class SyslogConfiguration {
    @Bean
    public SyslogService syslogService() {
        ApplicationPid applicationPid = new ApplicationPid();

        SyslogService service = new SyslogService();
        service.setAppName(getAppName());
        service.setFacility(getFacility());
        service.setPid(applicationPid.toString());

        return service;
    }

    @Bean
    public SyslogObserver syslogObserver() {
        return new SyslogObserver();
    }

    @Bean
    public SyslogMessageSender messageSender() {
        AbstractSyslogMessageSender messageSender;

        if (isUdp()) {
            messageSender = new UdpSyslogMessageSender();
        } else {
            messageSender = new TcpSyslogMessageSender();
            ((TcpSyslogMessageSender)messageSender).setSsl(isSsl());
            ((TcpSyslogMessageSender)messageSender).setMaxRetryCount(0);    // retrying is done using spring-retry
        }

        messageSender.setSyslogServerHostname(getHostName());
        messageSender.setSyslogServerPort(getPort());
        messageSender.setMessageFormat(getMessageFormat()); // optional, default is RFC 3164
        return messageSender;
    }

    protected abstract boolean isUdp();
    protected abstract boolean isSsl();

    protected abstract String getAppName();
    protected abstract Facility getFacility();

    protected abstract String getHostName();
    protected abstract int getPort();

    protected abstract MessageFormat getMessageFormat();
}
