![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/solugo/gitversion/release.yml?style=for-the-badge)
![GitHub Release](https://img.shields.io/github/v/release/solugo/gitversion?style=for-the-badge)
![GitHub License](https://img.shields.io/github/license/solugo/gitversion?style=for-the-badge)

## Execute in pipeline

### Latest Version
```bash
curl -Ls https://solugo.github.io/gitversion/run.sh | bash
```

### Specific Version And Arguments
```bash
curl -Ls https://solugo.github.io/gitversion/run.sh | GITVERSION="<version>" ARGS="<args>" bash
```

## Download

### Latest Version
```bash
curl -LsO https://github.com/solugo/gitversion/releases/latest/download/gitversion
```

### Specific Version
```bash
curl -LsO https://github.com/solugo/gitversion/releases/download/<version>/gitversion
```
