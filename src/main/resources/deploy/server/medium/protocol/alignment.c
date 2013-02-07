/*
 * @author Philipp Schlaefer
 * @since 01.02.2013
 */

//void set_unaligned ( int *target, int *data ) {
//    int offset, i;
//    char *byte, *res;
//
//    offset = ((int)target) % 4;
//    if (offset != 0) {
//        byte = (void*)data;
//        res = (void*)target;
//        for (i=0; i<4; i++) *(res++) = ((*(byte++)) & 0xFF);
//    } else *target = *data;
//}
//
//int get_unaligned ( int *data ) {
//    unsigned int offset, res, tmp;
//    int i;
//    char *byte;
//
//    offset = ((int)data) % 4;
//    if (offset != 0) {
//        byte = (void*)data;
//        res = 0;
//        for (i=0; i<4; i++) {
//            // make sure only rightmost 8bit are processed
//            tmp = (*(byte++)) & 0xFF;
//            // shift the value to the correct position
//            tmp <<= (i*8);
//            // sum up the 32bit value
//            res += tmp;
//        }
//        return res;
//    }
//    return *data;
//}
