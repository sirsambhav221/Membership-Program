package com.firstclub.membership;

import com.firstclub.membership.api.MembershipApiServer;
import com.firstclub.membership.service.MembershipService;

public final class Application {
    private Application() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        MembershipApiServer server = MembershipApiServer.create(port, new MembershipService());
        server.start();
        System.out.println("Membership API running at http://localhost:" + port);
    }
}
