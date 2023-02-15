import argparse
import textwrap


arg_parser = argparse.ArgumentParser(
    formatter_class=argparse.RawDescriptionHelpFormatter,
    description="Mayat",
    epilog=textwrap.dedent(
        """
        Explain:
            The script will find all code under the path:
                <DIR>/<any dirname>/<SUBPATH>

            For example, `python3 anubis_pd.py -d /home/homework -p dir1/prog.c` will match:
                /home/homework/<any dirname>/dir1/prog.c
        """
    ),
)
arg_parser.add_argument(
    "source_filenames",
    nargs='+',
    help="The source code files you want to check."
)
arg_parser.add_argument(
    "-f",
    dest="function_name",
    default="*",
    help="The specific function you want to check."
)
arg_parser.add_argument(
    "-o",
    dest="output_format",
    default="TXT",
    help="The format of the output. Default TXT."
)
arg_parser.add_argument(
    "-a",
    dest="list_all",
    action="store_true"
)
arg_parser.add_argument(
    "--threshold",
    type=int,
    default=5,
    help="The threshold value controlling the granularity of the matching. Default 5.",
)