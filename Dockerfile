FROM alpine
COPY ./build/bin/native/releaseExecutable/gitversion.kexe ./gitversion
ENTRYPOINT ["./gitversion"]