FROM pandeiro/lein
WORKDIR /lomake-editori
RUN lein upgrade
ADD config/docker-native.edn config/docker-native.edn
ADD config/defaults.edn config/defaults.edn
ADD config/dev.edn config/dev.edn
ADD templates templates
ADD project.clj .
ADD resources resources
ADD oph-configuration oph-configuration
ADD src src
RUN lein resource
RUN lein cljsbuild once hakija-min
RUN lein cljsbuild once virkailija-min
RUN lein less once
RUN lein uberjar
ENV CONFIG config/docker-native.edn
EXPOSE 3449
EXPOSE 3450
EXPOSE 8350
EXPOSE 8351
ENTRYPOINT ["java","-jar","target/ataru.jar"]
