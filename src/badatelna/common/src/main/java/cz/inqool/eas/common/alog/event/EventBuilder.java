package cz.inqool.eas.common.alog.event;

import cz.inqool.eas.common.authored.user.UserReference;
import cz.inqool.eas.common.module.ModuleDefinition;
import cz.inqool.eas.common.module.ModuleReference;
import cz.inqool.eas.common.module.Modules;
import cz.inqool.eas.common.security.User;
import cz.inqool.eas.common.storage.file.File;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

public class EventBuilder {
    private HttpServletRequest request;

    public EventCreate startup() {
        EventCreate event = new EventCreate();
        event.setMessage("Systém nastartoval");
        event.setSeverity(EventSeverity.INFO);
        event.setModule(ModuleReference.of(Modules.EAS));
        event.setSyslog(true);

        event.setSourceType(EventSourceType.SYSTEM);

        return event;
    }

    public EventCreate shutdown() {
        EventCreate event = new EventCreate();
        event.setMessage("Systém se vypíná");
        event.setSeverity(EventSeverity.INFO);
        event.setModule(ModuleReference.of(Modules.EAS));
        event.setSyslog(true);

        event.setSourceType(EventSourceType.SYSTEM);

        return event;
    }

    public EventCreate successfulLogin(User user) {
        EventCreate event = new EventCreate();
        event.setMessage("Úspěšné přihlášení");
        event.setIpAddress(getRequestIpAddress());
        event.setSeverity(EventSeverity.INFO);
        event.setSyslog(true);

        if (user != null) {
            event.setSourceType(EventSourceType.USER);
            event.setUser(new UserReference(user.getId(), user.getName()));
        } else {
            event.setSourceType(EventSourceType.SYSTEM);
        }

        return event;
    }

    public EventCreate failedLogin(User user) {
        EventCreate event = new EventCreate();
        event.setMessage("Neúspěšné přihlášení");
        event.setIpAddress(getRequestIpAddress());
        event.setSeverity(EventSeverity.WARN);
        event.setSyslog(true);

        if (user != null) {
            event.setSourceType(EventSourceType.USER);
            event.setUser(new UserReference(user.getId(), user.getName()));
        } else {
            event.setSourceType(EventSourceType.SYSTEM);
        }
        return event;
    }

    public EventCreate logout(User user) {
        EventCreate event = new EventCreate();
        event.setMessage("Úspěšné odhlášení");
        event.setIpAddress(getRequestIpAddress());
        event.setSeverity(EventSeverity.INFO);
        event.setSyslog(true);

        if (user != null) {
            event.setSourceType(EventSourceType.USER);
            event.setUser(new UserReference(user.getId(), user.getName()));
        } else {
            event.setSourceType(EventSourceType.SYSTEM);
        }

        return event;
    }

    public EventCreate logoutAutomatic(User user) {
        EventCreate event = new EventCreate();
        event.setMessage("Automatické odhlášení");
        event.setIpAddress(getRequestIpAddress());
        event.setSeverity(EventSeverity.INFO);
        event.setSyslog(true);

        event.setSourceType(EventSourceType.SYSTEM);

        if (user != null) {
            event.setUser(new UserReference(user.getId(), user.getName()));
        }

        return event;
    }

    public EventCreate deleting(Object object, UserReference user, ModuleDefinition module) {
        EventCreate event = new EventCreate();
        event.setMessage("Mazání objektu " + object);
        event.setIpAddress(getRequestIpAddress());
        event.setModule(ModuleReference.of(module));
        event.setSeverity(EventSeverity.INFO);
        event.setSyslog(true);

        if (user != null) {
            event.setSourceType(EventSourceType.USER);
            event.setUser(user);
        } else {
            event.setSourceType(EventSourceType.SYSTEM);
        }

        return event;
    }

    public EventCreate forbiddenUrl(String url, UserReference user) {
        EventCreate event = new EventCreate();
        event.setMessage("Nepovolený přístup k url " + url);
        event.setIpAddress(getRequestIpAddress());
        event.setSeverity(EventSeverity.WARN);
        event.setSyslog(true);

        if (user != null) {
            event.setSourceType(EventSourceType.USER);
            event.setUser(user);
        } else {
            event.setSourceType(EventSourceType.SYSTEM);
        }

        return event;
    }

    public EventCreate foundVirus(File file, UserReference user) {
        EventCreate event = new EventCreate();
        event.setMessage("Nalezen vírus v souboru " + file);
        event.setIpAddress(getRequestIpAddress());
        event.setSeverity(EventSeverity.WARN);
        event.setSyslog(true);

        if (user != null) {
            event.setSourceType(EventSourceType.USER);
            event.setUser(user);
        } else {
            event.setSourceType(EventSourceType.SYSTEM);
        }

        return event;
    }

    public String getRequestIpAddress() {
        try {
            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }

            return ipAddress;
        } catch (Exception e) {
            return null;
        }
    }

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
}
