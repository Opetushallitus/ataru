source-to-image:
	yum install -y wget
	wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
	chmod +x lein
	./lein cljsbuild once hakija-min
	./lein less once
	./lein cljsbuild once virkailija-min
	./lein resource
	./lein uberjar
