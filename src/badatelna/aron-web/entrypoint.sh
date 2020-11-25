#!/bin/sh

echo "" > .yarnrc.yml

echo '--- run yarn set version berry'
yarn set version berry

echo '--- run yarn plugin import workspace-tools'
yarn plugin import workspace-tools


echo 'bstatePath: "/yarn/build-state.yml"
cacheFolder: "/yarn/cache"
globalFolder: "/yarn/global"
virtualFolder: "/yarn/$$virtual"
pnpUnpluggedFolder: "/yarn/unplugged"
packageExtensions:
  fork-ts-checker-webpack-plugin@*:
    peerDependenciesMeta:
      typescript:
        optional: true
  react-draggable@*:
    peerDependenciesMeta:
      "@types/react":
        optional: true
  react-intl@*:
    peerDependenciesMeta:
      "@types/react":
        optional: true
  webpack@*:
    peerDependencies:
      webpack-cli: "*"
    peerDependenciesMeta:
      webpack-cli:
        optional: true
' >> .yarnrc.yml


echo '--- run yarn install'
yarn install

yarn watch