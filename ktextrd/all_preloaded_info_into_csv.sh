#!/bin/bash

# Unpack all apk files under /priv-app/ in a image into a `output` directory
# Then check all apk files and output badging parameters into standard output as csv format.
# It helps to survey the preloaded apps in a image.
# The command take a image name as an argument

exec 2>> packagecheck_errors.log

# Function to display usage
usage() {
  echo "Usage: $0 <system_image_path>"
  exit 1
}
# Function to check if a command is available
check_command() {
  local cmd_name="$1"
  local cmd_purpose="$2" # Optional: for a more descriptive error message

  if ! command -v "$cmd_name" &> /dev/null; then
    echo "Error: Command '$cmd_name' not found."
    if [ -n "$cmd_purpose" ]; then
      echo "$cmd_purpose"
    fi
    echo "Please install it or ensure it's in your PATH."
    exit 1
  fi
  echo "Command '$cmd_name' is available." # Confirmation message
}

extract_all_matching_details() {
  local pattern_line="$1"
  local extract_spec="$2"
  local results=() # 結果を格納する配列
  grep "$pattern_line" | while IFS= read -r input_line; do
    if [ -n "$input_line" ]; then
      local type=$(echo "$extract_spec" | cut -d':' -f1)
      local spec=$(echo "$extract_spec" | cut -d':' -f2-)
      local current_result=""
      #echo "$input_line,$type,$spec"
      if [[ "$type" == "FIELD" ]]; then
        local field_num=$(echo "$spec" | cut -d':' -f1)
        local delimiter_char=$(echo "$spec" | cut -d':' -f2)
        if [ -z "$delimiter_char" ]; then delimiter_char=" "; fi
        if [[ "$delimiter_char" == "'" ]]; then
          current_result=$(echo "$input_line" | awk -F"'" -v fn="$field_num" '{print $fn}')
        else
          current_result=$(echo "$input_line" | awk -F"$delimiter_char" -v fn="$field_num" '{print $fn}')
        fi
        #echo $current_result
      elif [[ "$type" == "REGEX" ]]; then
        current_result=$(echo "$input_line" | sed -n "s/.*${spec}.*/\1/p")
      else
        echo "Error: Invalid extract_spec type for line: $input_line" >&2
        continue
      fi
      if [ -n "$current_result" ]; then
        #results+=("$current_result")
        echo $current_result
      fi
    fi
  done
  #echo "${results[@]}"
  #printf "%s\n" "${results[@]}"
}

extract_badging_info() {
  local pattern_line="$1"
  local extract_spec="$2"
  local input_line
  local result=""
  input_line=$(grep "$pattern_line" -m 1)
  if [ -n "$input_line" ]; then
    local type=$(echo "$extract_spec" | cut -d':' -f1)
    local spec=$(echo "$extract_spec" | cut -d':' -f2-)

    if [[ "$type" == "FIELD" ]]; then
      local field_num=$(echo "$spec" | cut -d':' -f1)
      local delimiter_char=$(echo "$spec" | cut -d':' -f2)
      if [ -z "$delimiter_char" ]; then
          delimiter_char=" "
      fi
      if [[ "$delimiter_char" == "'" ]]; then
        result=$(echo "$input_line" | awk -F"'" -v fn="$field_num" '{print $fn}')
      else
        result=$(echo "$input_line" | awk -F"$delimiter_char" -v fn="$field_num" '{print $fn}')
      fi
    elif [[ "$type" == "REGEX" ]]; then
      result=$(echo "$input_line" | sed -n "s/.*${spec}.*/\1/p")
    else
      echo "Error: Invalid extract_spec type. Use FIELD or REGEX." >&2
      return 1
    fi
  fi
  echo "$result"
}


# Check for input argument
if [ -z "$1" ]; then
  usage
fi

# --- Prerequisite Checks ---
echo "Checking prerequisites..."
check_command "java" "Java is required, often for apksigner to function correctly."
check_command "aapt2" "aapt2 is required to verify APK fiies. It's part of the Android SDK Build Tools."

SYSTEM_IMAGE="$1"
DIRECTORY_NAME="output"
[ -d "$DIRECTORY_NAME" ] || { echo "Creating directory '$DIRECTORY_NAME'..." && mkdir "$DIRECTORY_NAME"; }

echo "Extracting APKs (assuming ktextrd.sh handles this and places them in $DIRECTORY_NAME)..."
./ktextrd.sh $SYSTEM_IMAGE -wx *.apk:$DIRECTORY_NAME &> /dev/null
#./ktextrd.sh $SYSTEM_IMAGE -wx *.apk:$DIRECTORY_NAME
echo "apk_name,package_name,app_label,permissions"
find output -type f -name "*.apk" -print0 | while IFS= read -r -d $'\0' full_file_path; do
   #echo "Full file path: $full_file_path"
   filename="${full_file_path##*/}"
   package_name=$(aapt2 dump packagename "$full_file_path")
   badging=$(aapt2 dump badging "$full_file_path" 2>/dev/null)

   if [[ "$full_file_path" == *priv-app* ]]; then
     app_label=$(echo "$badging" | extract_badging_info "application-label:" "FIELD:2:'")
     permissions=$(echo "$badging" | extract_all_matching_details "uses-permission: name=" "FIELD:2:'")
     echo $badging
   fi
done


