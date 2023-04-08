import sys
import json

json_result = json.load(sys.stdin)

for entry in json_result["result"]:
    sub_A_dirs = entry["submission_A"].split('/')
    sub_B_dirs = entry["submission_B"].split('/')

    cf1_subdirectory = sub_A_dirs[-2]
    cf1_filename = sub_A_dirs[-1]
    cf2_subdirectory = sub_B_dirs[-2]
    cf2_filename = sub_B_dirs[-1]

    for r in entry["overlapping_ranges"]:
        cf1_startline = r["A_start_pos"][0]
        cf1_endline = r["A_end_pos"][0]
        cf2_startline = r["B_start_pos"][0]
        cf2_endline = r["B_end_pos"][0]

        print(f"{cf1_subdirectory},{cf1_filename},{cf1_startline},{cf1_endline},{cf2_subdirectory},{cf2_filename},{cf2_startline},{cf2_endline}")