# submitit

This project is in development

## Usage

Submitit is used to submit presentations to ems-redux.

# Setup

# Dependencies
#### The following additional systems are needed:
* Ems-redux : Submitit has no internal database . All data are read from and writtten to ems-redux
* Smtp-server: Needed to send emails


# Config file
Full path to the configuration file must be given in the system propery SUBMITIT_SETUP_FILE.
The setupfile must be on the following format:
property=value

### The following values can be set:
* port: Port that submitit will run on. Default is 8080
* hostname: The hostname to the smtp server
* smtpport: The port to the smtp server
* user: User to logon to smtp (if any)
* password: Password to smtp (if any)
* mailSsl: Use ssl to connect to smtp
* smtpPort: Port to the smtp server
* serverHostname: The hostname of the webserver. ie. http://www.java.no/submitit
* mailFrom: The sender address of generated mails ie. program@java.no
* emsSubmitTalk: The url where the talk is to be submitted in ems. Talk should be submitted to a specific event. I.e. http://www.java.no/ems/server/events/<event-id>/sessions
* emsUser: User to use ems
* emsPassword: Password to ems
* photoCopyDir: If present a copy of all photos will be copied here
* closing-time: The date call for speakers will be closed and a password will be required to submit a new proposal. Date format yyyyMMddHHmmss
* close-password: The password required to submit new talks after the deadline


## Frontend developer mode
Frontend developer mode can be used to debug frontend pages. The server will only return dummy data.

### Setup:
1. Install leiningen (https://github.com/technomancy/leiningen)
3. Set system variable SUBMITIT_FRONTEND_MODE to true (export SUBMITIT_FRONTEND_MODE=true)
4. lein run

You will no have the following pages availible:
http://localhost:8080/index.html
http://localhost:8080/talkDetail.html

## License

Copyright (C) 2015 javaBin

Distributed under the Eclipse Public License, the same as Clojure.


## Todos



