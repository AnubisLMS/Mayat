import json


def print_str(result_dict: dict):
    print(result_dict["current_datetime"])
    print()

    print(f"Checking function: {result_dict['function']}")
    print()

    print("Warnings:")
    for warning in result_dict["warnings"]:
        print(warning)
    print()
        
    print(f"Checker result:")
    for entry in sorted(result_dict["result"], key=lambda x: x["similarity"], reverse=True):
        print(f"{entry['submission_A']} - {entry['submission_B']}:\t{entry['similarity']:%}")
    print()
    
    print(f"{result_dict['execution_time']}s")


def print_result(result_dict, format, list_all):
    if not list_all:
        result_dict["result"] = list(
            filter(lambda x: x["similarity"] > 0, result_dict["result"])
        )

    if format == "JSON":
        print(json.dumps(result_dict, indent=4))
    else:
        if format != "TXT":
            print("Unknown output format. Using TXT format")
        print_str(result_dict)
