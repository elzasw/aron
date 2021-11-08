# Instalace balicku pro vyvoj frontendu

v korenovem adresari (badatelna) pustit prikaz:
```
    yarn install
```

# Zprovozneni ve VSCode 

Ve VSCode otevrit workspace ze souboru:
   `File > Open workspace from file...` -> `./.vscode/badatelna.code-workspace`

## Sestaveni zavislosti pro aron-web

v `./common-web` spustit:
```
    yarn run build
```

a nechat dobehnout (nic nevypisuje)

# Spusteni backend

- Sestaveni aplikace: 
```
./gradlew buildAll
```
- Spusteni serveru: 
```
docker compose up
```

# Spusteni frontend dev verze

## aron-web
v `./aron-web` spustit: 
```
yarn watch
```
v `./aron-dev` spustit: 
```
docker compose up
```

## common-web
    TODO - potreba doplnit

