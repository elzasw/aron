@echo off
REM ---------------------------------------------------------------------------
REM Builds the administrator documentation (HTML) with a Sphinx Docker image.
REM
REM The image name comes from the SPHINXDOC_IMAGE environment variable, set in
REM the gitignored set-env.bat (see set-env.bat.template). Without it the
REM public sphinxdoc/sphinx image is used and the RTD theme is installed on
REM the fly (internet required).
REM
REM This repository is publicly mirrored: registry credentials and internal
REM registry URLs never belong in this script. Logging in to a private
REM registry (docker login) is a manual, one-time step outside the repo.
REM ---------------------------------------------------------------------------

pushd %~dp0

if exist "..\set-env.bat" call "..\set-env.bat"

if "%SPHINXDOC_IMAGE%" == "" goto public

echo Building documentation with %SPHINXDOC_IMAGE% ...
docker run --rm -v "%cd%":/data -w /data %SPHINXDOC_IMAGE% make html
goto end

:public
echo SPHINXDOC_IMAGE not set - using the public sphinxdoc/sphinx image...
docker run --rm -v "%cd%":/data -w /data sphinxdoc/sphinx:latest sh -c "pip install --quiet sphinx-rtd-theme && make html"

:end
echo Output: build\html\index.html
popd
