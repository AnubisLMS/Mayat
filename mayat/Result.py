import json

def print_str(result_dict):
    print(result_dict["current_datetime"])
    print(result_dict["command"])
    print()

    print("Things to check:")
    for cp in result_dict["checkpoints"]:
        print(f"{cp['path']}: {' | '.join([f'{id[1]} {id[0]}' for id in cp['identifiers']])}")

    for result in result_dict["checkpoint_results"]:
        print()
        print()
        print(result["subpath"])
        print()

        for warning in result["warnings"]:
            print(warning)
        print()
        
        for pnk_result in result["path_name_kind_result"]:
            print(f"Results for {result['subpath']}: {pnk_result['name']}:{pnk_result['kind']}")
            print()

            for entry in sorted(pnk_result["entries"], key=lambda x: x["similarity"], reverse=True):
                print(f"{entry['submission_A']} - {entry['submission_B']}:\t{entry['similarity']:%}")
            print()
    
    print(f"{result_dict['execution_time']}s")

def print_result(result_dict, format):
    if format == "JSON":
        print(json.dumps(result_dict, indent=4))
    else:
        if format != "TXT":
            print("Unknown output format. Using TXT format")
        print_str(result_dict)