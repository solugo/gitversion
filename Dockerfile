FROM gcr.io/distroless/static
COPY ./build/bin/linuxX64/releaseExecutable/gitversion.kexe ./gitversion
CMD chmod a+x ./gitversion
ENTRYPOINT ["./gitversion", "--help"]