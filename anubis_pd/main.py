import clang.cindex
import sys
from anubis_pd.driver import driver
from anubis_pd.args import parse_args
from anubis_pd.C_AST import C_AST

def main():

    args = parse_args()

    # /Library/Developer/CommandLineTools/usr/lib
    if args.libclang_path is not None:
        clang.cindex.Config.set_library_path(args.libclang_path)
        index = clang.cindex.Index.create()
    else:
        try:
            index = clang.cindex.Index.create()
        except clang.cindex.LibclangError:
            print("Cannot find libclang. Please specify its path using --libclang argument")
            sys.exit()
    driver(C_AST, args)

if __name__ == "__main__":
    main()