#include <stdio.h>
int main(){
    char buf[200];
    int i = 3;
    printf("23\n");
    while(i--){
        scanf("%s",buf);
        printf("%s\n",buf);
        fflush(stdout);
        printf("1\n");
    }
}
