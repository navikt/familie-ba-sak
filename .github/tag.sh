#!/usr/bin/env bash

#0: Sjekk at du er på master
current_branch=$(git branch | grep \* | cut -d ' ' -f2)
master_branch="master"

if ! [ "$current_branch" == "$master_branch" ]; then
    printf "Må være på master-branch for å tagge. Avslutter script\n"
    exit 1
fi

#1: Hent nåværende versjon/tag
git fetch --prune --tags

version=$(git describe --abbrev=0 --tags)
current_major=${version:1:1}
current_minor=${version:3}
printf "Nåværende versjon: $version\n"

#2: Sjekk at lokal master er lik master på remote
remote_master_SHA=$(git rev-parse origin/master)
master_SHA=$(git rev-parse HEAD)

if ! [ $remote_master_SHA == $master_SHA ]; then
    printf "Du kan ikke tagge denne commiten: den er ikke lik origin/master.\n"
    exit 1
fi

#3: Hent major/minor-flagg fra kommandolinjen og bump i henhold til det
while getopts ":Mm" opt
do
  case $opt in
    M ) major='true';;
    m ) minor='true';;
  esac
done

shift $(($OPTIND - 1))

if [ -z $major ] && [ -z $minor ]; then
    printf "usage: Spesifiser om du vil bumpe major og/eller minor med hhv flaggene -M og -m. Major/minor vil da bumpes med 1 fra siste release-tag.\n"
    exit 1
fi

new_major=$current_major
new_minor=$current_minor

if [ "$major" = true ]; then
    ((new_major++))
    new_minor=0
fi

if [ "$minor" = true ]; then
    ((new_minor++))
fi

next_version="v$new_major.$new_minor"
printf "Neste versjon blir: $next_version\n"

#4: Tag git commit med ny versjon
git tag -a $next_version -m "Release av versjon $next_version"

#5: Push tag
git push origin $next_version