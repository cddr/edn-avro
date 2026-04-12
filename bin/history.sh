#!/usr/bin/env bash

# 1. Wrap the logic in a function
generate_history() {

	REMOTE_TAGS=$(git ls-remote --tags origin | awk '{print $2}' | sed 's|refs/tags/||' | sed 's/\^{}//')
  tags=$(git for-each-ref --format='%(*refname:short)|%(taggerdate:short)' refs/tags | tac | grep -v '|^')
  LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)

  echo -e "\n\033[0;34m📜 Change Summary \033[0m"
  echo "──────────────────────────────────────────────"

  # Unreleased Changes
  echo -e "\033[1;33m🚧 Unreleased Changes (HEAD)\033[0m"
  if [ -z "$LATEST_TAG" ]; then
    git log --oneline | sed 's/^/   • /'
  else
    COMMITS_SINCE=$(git log "${LATEST_TAG}..HEAD" --oneline)
    if [ -z "$COMMITS_SINCE" ]; then
      echo "   (Clean - Ready for release)"
    else
      echo "$COMMITS_SINCE" | sed 's/^/   • /'
    fi
  fi

  # Historical Tags
  if [ -n "$tags" ]; then
    mapfile -t tag_array <<< "$tags"
    for (( i=0; i<${#tag_array[@]}; i++ )); do
      IFS='|' read -r tag date <<< "${tag_array[i]}"
      prev_tag=$(echo "${tag_array[i+1]}" | cut -d'|' -f1)
			bare_tag=$(echo $tag | tr '^{}' ' ')

			# Check if current tag exists in our REMOTE_TAGS list
      if echo "$REMOTE_TAGS" | grep -qxw "$tag"; then
        STATUS_ICON="\033[0;32m[Upstream]\033[0m"
      else
        STATUS_ICON="\033[0;31m[Local Only]\033[0m"
      fi

      echo -e "\n\033[1;33m📍 $bare_tag [$date]\033[0m $STATUS_ICON"
      if [ -n "$prev_tag" ]; then
        git log "${prev_tag}..${tag}" --oneline | sed 's/^/   • /'
      else
        git log "${tag}" --oneline | sed 's/^/   • /'
      fi
    done
  fi
  echo -e "\n──────────────────────────────────────────────"
}

# Self-paging logic
if [[ -t 1 ]]; then
	generate_history | ${PAGER:less -RF}
fi
